import boto3
import threading
import time
import sys

TABLE_NAME = "t_console_edge_store"

# takes one optional argument - the number number of threads to run
NUM_THREADS = int(sys.argv[1] or 1) if len(sys.argv) == 2 else 1

print "NUM_THREADS: %s" % NUM_THREADS

# Used to explicitly test with a single hot partition
HK = bytearray('one_partition')

class MyTask(threading.Thread):
  def __init__(self, thread_number):
        threading.Thread.__init__(self)
        self.threadID = "thread %s" %  thread_number
        self.thread_number = thread_number

  def run(self):
    print '%s: Starting... setup table: %s' % (self.threadID, TABLE_NAME)

    time.sleep(float(self.thread_number))

    dynamodb = boto3.resource('dynamodb')

    table = dynamodb.Table(TABLE_NAME)


    # Create the DynamoDB table.
    # Modeled after Titan GraphDb Edge Store table for dynamo backend
    table = table or dynamodb.create_table(
        TableName='t_mock_edge_store',
        KeySchema=[
            {
                'AttributeName': 'hk',
                'KeyType': 'HASH'
            },
            {
                'AttributeName': 'rk',
                'KeyType': 'RANGE'
            }
        ],
        AttributeDefinitions=[
            {
                'AttributeName': 'hk',
                'AttributeType': 'B'
            },
            {
                'AttributeName': 'rk',
                'AttributeType': 'B'
            }
        ],
        ProvisionedThroughput={
            'ReadCapacityUnits': 1000,
            'WriteCapacityUnits': 20000
        }
    )

    print "%s: Waiting for active status" % self.threadID

    # Wait until the table exists.
    table.meta.client.get_waiter('table_exists').wait(TableName=TABLE_NAME)

    # Print out some data about the table.
    print "%s: Item count %d" % (self.threadID, table.item_count)


    print "%s: Putting items!" % self.threadID
    icnt = 1

    with open("/dev/urandom","rb") as f:
      with table.batch_writer() as batch:
        t0 = tn = time.time()
        while True:
          batch.put_item(
            # Generate random byte array data for partition key, range key and value to test throughput for
            # a balanced writes across the dynamo partitions.
            Item={'hk':bytearray(f.read(12)),'rk':bytearray(f.read(18)),'v':bytearray(f.read(30))}
            # to examine behavior with a hot partition, use this line instead:
            # Item={'hk':HK,'rk':bytearray(f.read(18)),'v':bytearray(f.read(300))}
            )
          if icnt % 1000 == 0:
            tt = time.time()
            print "%s: Batch put %d items!, avg_rate %f, curr_rate %f" % (self.threadID, icnt, icnt/(tt - t0), 1000/(tt - tn))
            tn = tt
          icnt +=1

threads = [MyTask(e).start() for x,e in enumerate(range(NUM_THREADS))]







