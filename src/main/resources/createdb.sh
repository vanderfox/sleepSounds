aws --profile vanderfox --region us-east-1 dynamodb create-table --table-name sleepsounds --attribute-definitions AttributeName=soundName,AttributeType=S --key-schema AttributeName=soundName,KeyType=HASH --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
aws --profile vanderfox --region us-east-1 dynamodb create-table --table-name sleepsounds_playback_state --attribute-definitions AttributeName=token,AttributeType=S --key-schema AttributeName=token,KeyType=HASH --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

