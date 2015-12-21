import boto3
import threading
import time

TABLE_NAME = "t_console_edge_store"

HK = bytearray('one_partition')

class MyTask(threading.Thread):
  def __init__(self, threadID):
        threading.Thread.__init__(self)
        self.threadID = threadID

  def run(self):
    print '%s: Starting... setup table: %s' % (self.threadID, TABLE_NAME)

    time.sleep(float(self.threadID))

    dynamodb = boto3.resource('dynamodb')

    table = dynamodb.Table(TABLE_NAME)


    # Create the DynamoDB table.
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
            Item={'hk':bytearray(f.read(12)),'rk':bytearray(f.read(18)),'v':bytearray(f.read(30))}
            # Item={'hk':HK,'rk':bytearray(f.read(18)),'v':bytearray(f.read(300))}
            )
          if icnt % 1000 == 0:
            tt = time.time()
            print "%s: Batch put %d items!, avg_rate %f, curr_rate %f" % (self.threadID, icnt, icnt/(tt - t0), 1000/(tt - tn))
            tn = tt
          icnt +=1

threads = [MyTask("thread %s" % e).start() for x,e in enumerate(range(8))]







