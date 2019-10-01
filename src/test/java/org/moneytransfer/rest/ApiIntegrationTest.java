package org.moneytransfer.rest;

import io.javalin.plugin.json.JavalinJson;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.moneytransfer.service.AccountDto;
import org.moneytransfer.service.MoneyServiceError;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class ApiIntegrationTest {

    private static URI SERVER_URI = URI.create("http://localhost:9001");

    private HttpClient httpClient = HttpClient.newBuilder().build();

    @BeforeClass
    public static void beforeAll() {
        App.start(9001);
    }

    @AfterClass
    public static void tearDown() {
        App.stop();
    }

    private HttpResponse<String> createAccount() throws Exception {
        var request = HttpRequest.newBuilder(SERVER_URI.resolve("/accounts"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getAccount(long id) throws Exception {
        var request = HttpRequest.newBuilder(SERVER_URI.resolve("/accounts/" + id))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> deposit(long id, BigDecimal amount) throws Exception {
        var body = JavalinJson.toJson(new MoneyOperationRequest(amount));
        var request = HttpRequest.newBuilder(SERVER_URI.resolve("/accounts/" + id + "/deposit"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> withdraw(long id, BigDecimal amount) throws Exception {
        var body = JavalinJson.toJson(new MoneyOperationRequest(amount));
        var request = HttpRequest.newBuilder(SERVER_URI.resolve("/accounts/" + id + "/withdraw"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> transfer(long from, long to, BigDecimal amount) throws Exception {
        var body = JavalinJson.toJson(new MoneyOperationRequest(amount));
        var request = HttpRequest.newBuilder(SERVER_URI.resolve("/transfers/" + from + "/" + to))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private AccountDto doGetAccount(long id) throws Exception {
        var getAccountResponse = getAccount(id);
        return JavalinJson.fromJson(getAccountResponse.body(), AccountInfoResponse.class).getAccount();
    }

    private AccountDto doCreateAccount() throws Exception {
        var getAccountResponse = createAccount();
        return JavalinJson.fromJson(getAccountResponse.body(), AccountInfoResponse.class).getAccount();
    }

    @Test
    public void accountIsCreated() throws Exception {
        var response = createAccount();
        assertEquals(201, response.statusCode());
        var result = JavalinJson.fromJson(response.body(), AccountInfoResponse.class);
        assertNull(result.getError());
        assertNull(result.getDescription());
        var account = result.getAccount();
        assertEquals(BigDecimal.ZERO, account.getBalance());
    }

    @Test
    public void getAccountSucceeds() throws Exception {
        var createdAccount = doCreateAccount();
        var getAccountResponse = getAccount(createdAccount.getId());
        assertEquals(200, getAccountResponse.statusCode());
        var result = JavalinJson.fromJson(getAccountResponse.body(), AccountInfoResponse.class);
        assertNull(result.getError());
        assertNull(result.getDescription());
        var gotAccount = result.getAccount();
        assertEquals(createdAccount.getId(), gotAccount.getId());
        assertEquals(createdAccount.getBalance(), gotAccount.getBalance());
    }

    @Test
    public void getAccountFailsIfIdIsUnknown() throws Exception {
        var getAccountResponse = getAccount(Long.MAX_VALUE);
        assertEquals(404, getAccountResponse.statusCode());
        var result = JavalinJson.fromJson(getAccountResponse.body(), AccountInfoResponse.class);
        assertNull(result.getAccount());
        assertEquals(MoneyServiceError.ACCOUNT_NOT_FOUND, result.getError());
    }

    @Test
    public void depositSucceeds() throws Exception {
        var accountId = doCreateAccount().getId();
        var n = BigDecimal.TEN;
        var depositResponse = deposit(accountId, n);
        assertEquals(200, depositResponse.statusCode());

        var depositResult = JavalinJson.fromJson(depositResponse.body(), MoneyOperationResponse.class);
        assertNull(depositResult.getError());

        assertEquals(n, doGetAccount(accountId).getBalance());
    }

    @Test
    public void depositFailsIfIdIsUnknown() throws Exception {
        var depositResponse = deposit(Long.MAX_VALUE, BigDecimal.ONE);
        assertEquals(404, depositResponse.statusCode());
        var result = JavalinJson.fromJson(depositResponse.body(), MoneyOperationResponse.class);
        assertEquals(MoneyServiceError.ACCOUNT_NOT_FOUND, result.getError());
    }

    @Test
    public void depositFailsIfAmountIsNotPositive() throws Exception {
        var accountId = doCreateAccount().getId();
        var depositResponse = deposit(accountId, BigDecimal.ONE.negate());
        assertEquals(400, depositResponse.statusCode());
        var depositResult = JavalinJson.fromJson(depositResponse.body(), MoneyOperationResponse.class);
        assertEquals(MoneyServiceError.AMOUNT_NOT_POSITIVE, depositResult.getError());

        assertEquals("State must be unchanged", BigDecimal.ZERO, doGetAccount(accountId).getBalance());
    }

    @Test
    public void withdrawSucceeds() throws Exception {
        var accountId = doCreateAccount().getId();
        var n = BigDecimal.TEN;
        deposit(accountId, n);
        var withdrawResponse = withdraw(accountId, BigDecimal.ONE);
        assertEquals(200, withdrawResponse.statusCode());

        var withdrawResult = JavalinJson.fromJson(withdrawResponse.body(), MoneyOperationResponse.class);
        assertNull(withdrawResult.getError());

        assertEquals(n.subtract(BigDecimal.ONE), doGetAccount(accountId).getBalance());
    }

    @Test
    public void withdrawFailsIfIdIsUnknown() throws Exception {
        var withdrawResponse = withdraw(Long.MAX_VALUE, BigDecimal.ONE);
        assertEquals(404, withdrawResponse.statusCode());
        var result = JavalinJson.fromJson(withdrawResponse.body(), MoneyOperationResponse.class);
        assertEquals(MoneyServiceError.ACCOUNT_NOT_FOUND, result.getError());
    }

    @Test
    public void withdrawFailsIfAmountIsNotPositive() throws Exception {
        var accountId = doCreateAccount().getId();
        var withdrawResponse = withdraw(accountId, BigDecimal.ONE.negate());
        assertEquals(400, withdrawResponse.statusCode());
        var withdrawResult = JavalinJson.fromJson(withdrawResponse.body(), MoneyOperationResponse.class);
        assertEquals(MoneyServiceError.AMOUNT_NOT_POSITIVE, withdrawResult.getError());

        assertEquals("State must be unchanged", BigDecimal.ZERO, doGetAccount(accountId).getBalance());
    }

    @Test
    public void withdrawFailsIfBalanceIsInsufficient() throws Exception {
        var accountId = doCreateAccount().getId();
        var withdrawResponse = withdraw(accountId, BigDecimal.ONE);
        assertEquals(403, withdrawResponse.statusCode());
        var withdrawResult = JavalinJson.fromJson(withdrawResponse.body(), MoneyOperationResponse.class);
        assertEquals(MoneyServiceError.INSUFFICIENT_BALANCE, withdrawResult.getError());

        assertEquals("State must be unchanged", BigDecimal.ZERO, doGetAccount(accountId).getBalance());
    }

    @Test
    public void transferSucceeds() throws Exception {
        var fromId = doCreateAccount().getId();
        var toId = doCreateAccount().getId();
        var n = BigDecimal.TEN;
        deposit(fromId, n);
        var transferResponse = transfer(fromId, toId, n);
        assertEquals(200, transferResponse.statusCode());

        var transferResult = JavalinJson.fromJson(transferResponse.body(), MoneyOperationResponse.class);
        assertNull(transferResult.getError());

        assertEquals(BigDecimal.ZERO, doGetAccount(fromId).getBalance());
        assertEquals(n, doGetAccount(toId).getBalance());
    }


    @Test
    public void transferFailsIfIdIsUnknown() throws Exception {
        var transferResponse = transfer(Long.MAX_VALUE, Long.MAX_VALUE - 1, BigDecimal.ONE);
        assertEquals(404, transferResponse.statusCode());
        var result = JavalinJson.fromJson(transferResponse.body(), MoneyOperationResponse.class);
        assertEquals(MoneyServiceError.ACCOUNT_NOT_FOUND, result.getError());
    }

    @Test
    public void transferFailsIfSameAccount() throws Exception {
        var accountId = doCreateAccount().getId();
        var transferResponse = transfer(accountId, accountId, BigDecimal.ONE);
        assertEquals(400, transferResponse.statusCode());
        var result = JavalinJson.fromJson(transferResponse.body(), MoneyOperationResponse.class);
        assertEquals(MoneyServiceError.SAME_ACCOUNT, result.getError());
        assertEquals("State must be unchanged", BigDecimal.ZERO, doGetAccount(accountId).getBalance());
    }

    @Test
    public void transferFailsIfAmountIsNotPositive() throws Exception {
        var fromId = doCreateAccount().getId();
        var toId = doCreateAccount().getId();
        var transferResponse = transfer(fromId, toId, BigDecimal.ONE.negate());
        assertEquals(400, transferResponse.statusCode());
        var result = JavalinJson.fromJson(transferResponse.body(), MoneyOperationResponse.class);
        assertEquals(MoneyServiceError.AMOUNT_NOT_POSITIVE, result.getError());
        assertEquals("State must be unchanged", BigDecimal.ZERO, doGetAccount(fromId).getBalance());
        assertEquals("State must be unchanged", BigDecimal.ZERO, doGetAccount(toId).getBalance());
    }

    @Test
    public void transferFailsIfBalanceIsInsufficient() throws Exception {
        var fromId = doCreateAccount().getId();
        var toId = doCreateAccount().getId();
        var transferResponse = transfer(fromId, toId, BigDecimal.ONE);
        assertEquals(403, transferResponse.statusCode());
        var result = JavalinJson.fromJson(transferResponse.body(), MoneyOperationResponse.class);
        assertEquals(MoneyServiceError.INSUFFICIENT_BALANCE, result.getError());
        assertEquals("State must be unchanged", BigDecimal.ZERO, doGetAccount(fromId).getBalance());
        assertEquals("State must be unchanged", BigDecimal.ZERO, doGetAccount(toId).getBalance());
    }
}
