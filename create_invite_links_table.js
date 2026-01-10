/**
 * Create DynamoDB table for Invite Links
 * Run this script once to create the table in AWS
 * 
 * Usage: node create_invite_links_table.js
 */

require('dotenv').config();
const { DynamoDBClient, CreateTableCommand, DescribeTableCommand } = require('@aws-sdk/client-dynamodb');

const client = new DynamoDBClient({ region: process.env.AWS_REGION || 'us-east-1' });

const tableName = 'Buddylynk_InviteLinks';

async function createTable() {
    try {
        // Check if table already exists
        try {
            await client.send(new DescribeTableCommand({ TableName: tableName }));
            console.log(`✅ Table '${tableName}' already exists!`);
            return;
        } catch (err) {
            if (err.name !== 'ResourceNotFoundException') {
                throw err;
            }
        }

        // Create table
        const command = new CreateTableCommand({
            TableName: tableName,
            KeySchema: [
                { AttributeName: 'linkId', KeyType: 'HASH' }  // Primary key
            ],
            AttributeDefinitions: [
                { AttributeName: 'linkId', AttributeType: 'S' },
                { AttributeName: 'groupId', AttributeType: 'S' },
                { AttributeName: 'code', AttributeType: 'S' }
            ],
            GlobalSecondaryIndexes: [
                {
                    IndexName: 'groupId-index',
                    KeySchema: [
                        { AttributeName: 'groupId', KeyType: 'HASH' }
                    ],
                    Projection: { ProjectionType: 'ALL' },
                    ProvisionedThroughput: {
                        ReadCapacityUnits: 5,
                        WriteCapacityUnits: 5
                    }
                },
                {
                    IndexName: 'code-index',
                    KeySchema: [
                        { AttributeName: 'code', KeyType: 'HASH' }
                    ],
                    Projection: { ProjectionType: 'ALL' },
                    ProvisionedThroughput: {
                        ReadCapacityUnits: 5,
                        WriteCapacityUnits: 5
                    }
                }
            ],
            ProvisionedThroughput: {
                ReadCapacityUnits: 5,
                WriteCapacityUnits: 5
            }
        });

        await client.send(command);
        console.log(`✅ Table '${tableName}' created successfully!`);
        console.log('');
        console.log('Table structure:');
        console.log('  - linkId (Primary Key) - UUID for the invite link');
        console.log('  - groupId (GSI) - The group this link belongs to');
        console.log('  - code (GSI) - Short code for the invite URL');
        console.log('  - url - Full invite URL');
        console.log('  - createdAt - When the link was created');
        console.log('  - createdBy - User ID who created the link');
        console.log('  - isActive - Whether the link is still valid');

    } catch (err) {
        console.error('❌ Error creating table:', err.message);
        process.exit(1);
    }
}

createTable();
