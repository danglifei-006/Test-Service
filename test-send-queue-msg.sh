

aws sqs send-message \
  --queue-url https://sqs.ap-southeast-1.amazonaws.com/846697434276/transactions-queue.fifo \
  --message-body '{
    "transactionId": "TEST-12345",
    "accountId": "ACCT-123",
    "amount": 15000.0,
    "location": "HighRiskCountry1",
    "timestamp": "2023-07-01T12:00:00Z",
    "merchantId": "MCH-TEST"
  }' \
  --message-group-id "transaction-group-1"


aws sqs send-message \
  --queue-url https://sqs.ap-southeast-1.amazonaws.com/846697434276/transactions-queue.fifo \
  --message-body '{
    "transactionId": "TEST-12346",
    "accountId": "ACCT-123",
    "amount": 15000.0,
    "location": "HighRiskCountry1",
    "timestamp": "2023-07-01T12:00:00Z",
    "merchantId": "MCH-TEST"
  }' \
  --message-group-id "transaction-group-1"


aws sqs send-message \
  --queue-url https://sqs.ap-southeast-1.amazonaws.com/846697434276/transactions-queue.fifo \
  --message-body '{
    "transactionId": "TEST-12347",
    "accountId": "ACCT-123",
    "amount": 15000.0,
    "location": "HighRiskCountry1",
    "timestamp": "2023-07-01T12:00:00Z",
    "merchantId": "MCH-TEST"
  }' \
  --message-group-id "transaction-group-1"



aws sqs send-message \
  --queue-url https://sqs.ap-southeast-1.amazonaws.com/846697434276/transactions-queue.fifo \
  --message-body '{
    "transactionId": "TEST-12348",
    "accountId": "ACCT-123",
    "amount": 15000.0,
    "location": "HighRiskCountry1",
    "timestamp": "2023-07-01T12:00:00Z",
    "merchantId": "MCH-TEST"
  }' \
  --message-group-id "transaction-group-1"



aws sqs send-message \
  --queue-url https://sqs.ap-southeast-1.amazonaws.com/846697434276/transactions-queue.fifo \
  --message-body '{
    "transactionId": "TEST-12349",
    "accountId": "ACCT-123",
    "amount": 15000.0,
    "location": "HighRiskCountry1",
    "timestamp": "2023-07-01T12:00:00Z",
    "merchantId": "MCH-TEST"
  }' \
  --message-group-id "transaction-group-1"