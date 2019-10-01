# money-transfer

Compile: `mvn compile`

Run tests: `mvn test`

Run app at port 8080: `mvn exec:java -Dexec.mainClass="org.moneytransfer.rest.App"`


API:

- `POST /accounts`
    - creates account
    - body: empty
    - result: `{"id": 0, "balance": 0}`
- `GET /accounts/:id`
    - gets account
    - result: `{"id": 0, "balance": 0}`
- `POST /accounts/:id/deposit`
    - deposits to account
    - body: `{"amount": 0}`
    - result: `{"error": MoneyServiceError, "description": String}`
- `POST /accounts/:id/withdraw`
    - withdraws from account
    - body: `{"amount": 0}`
    - result: `{"error": MoneyServiceError, "description": String}`
- `POST /transfers/:from/:to`
    - transfers from `from` account to `to` account
    - body: `{"amount": 0}`
    - result: `{"error": MoneyServiceError, "description": String}`
