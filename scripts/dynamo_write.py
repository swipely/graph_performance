import boto3
TABLE_NAME = "t_mock_edge_store"
print 'Starting... setup table: %s' % TABLE_NAME
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

print 'Waiting for active status'

# Wait until the table exists.
table.meta.client.get_waiter('table_exists').wait(TableName=TABLE_NAME)

# Print out some data about the table.
print "Item count %d" % table.item_count

with open("/dev/urandom","rb") as f:
  with table.batch_writer() as batch:
    while True:
      batch.put_item(
        Item={'hk':bytearray(f.read(12)),'rk':bytearray(f.read(18)),'v':bytearray(f.read(30))}
        )





