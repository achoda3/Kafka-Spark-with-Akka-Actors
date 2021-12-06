import json
import boto3
import requests
# Got this script set to run whenever a file is created in my s3 Bucket. 
# This then sends a post request to my Server
def lambda_handler(event, context):
    # TODO implement
    print("you lil bitch")
    print(event['Records'][0]['s3']['bucket']['arn'])
    print(event['Records'][0]['s3']['object']['key'])
    arn = event['Records'][0]['s3']['bucket']['arn']
    key = event['Records'][0]['s3']['object']['key']
    response = requests.post("https://3.135.198.10:80", json = {"bucket": arn, "key": key})
    return {
        'statusCode': 200,
        'body': json.dumps('Hello from Lambda!')
    }
