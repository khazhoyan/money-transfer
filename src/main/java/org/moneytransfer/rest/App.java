package org.moneytransfer.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.javalin.Javalin;
import io.javalin.plugin.json.JavalinJson;
import org.moneytransfer.repository.AccountRepository;
import org.moneytransfer.service.MoneyService;
import org.moneytransfer.service.MoneyServiceException;

public final class App {

    private static Javalin app;

    // Visible for tests
    static void start(int port) {
        var accountDao = new AccountRepository();
        var moneyService = new MoneyService(accountDao);
        app = Javalin.create().start(port);
        app.exception(ParamException.class, (e, ctx) -> {
            ctx.status(400);
            ctx.result(e.getMessage());
        }).exception(JsonMappingException.class, (e, ctx) -> {
            ctx.status(400);
            ctx.result("JsonMappingException: " + e.getMessage());
        }).exception(MoneyServiceException.class, (e, ctx) -> {
            switch (e.getErrorStatus()) {
                case SAME_ACCOUNT:
                case AMOUNT_NOT_POSITIVE:
                    ctx.status(400);
                    break;
                case INSUFFICIENT_BALANCE:
                    ctx.status(403);
                    break;
                case ACCOUNT_NOT_FOUND:
                    ctx.status(404);
                    break;
                default:
                    ctx.status(500);
            }
            ctx.json(MoneyOperationResponse.error(e.getErrorStatus(), e.getMessage()));
        }).get("/accounts/:id", ctx -> {
            var id = longPathParam(ctx, "id");
            var account = moneyService.getAccount(id);
            ctx.json(AccountInfoResponse.ok(account));
        }).post("/accounts", ctx -> {
            var account = moneyService.createAccount();
            ctx.status(201);
            ctx.json(AccountInfoResponse.ok(account));
        }).post("/accounts/:id/deposit", ctx -> {
            var id = longPathParam(ctx, "id");
            var request = JavalinJson.fromJson(ctx.body(), MoneyOperationRequest.class);
            var amount = request.getAmount();
            moneyService.deposit(id, amount);
            ctx.json(MoneyOperationResponse.ok("Deposit succeeded"));
        }).post("/accounts/:id/withdraw", ctx -> {
            var id = longPathParam(ctx, "id");
            var request = JavalinJson.fromJson(ctx.body(), MoneyOperationRequest.class);
            var amount = request.getAmount();
            moneyService.withdraw(id, amount);
            ctx.json(MoneyOperationResponse.ok("Withdraw succeeded"));
        }).post("/transfers/:from/:to", ctx -> {
            var from = longPathParam(ctx, "from");
            var to = longPathParam(ctx, "to");
            var request = JavalinJson.fromJson(ctx.body(), MoneyOperationRequest.class);
            var amount = request.getAmount();
            moneyService.transfer(from, to, amount);
            ctx.json(MoneyOperationResponse.ok("Transfer succeeded"));
        });
    }

    // Visible for tests
    static void stop() {
        app.stop();
    }

    public static void main(String[] args) {
        start(8080);
    }

    private static long longPathParam(io.javalin.http.Context ctx, String name) throws ParamException {
        var longStr = ctx.pathParam(name);
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException ex) {
            throw new ParamException(
                    String.format("Param '%s' is expected to be a number, but got '%s'", name, longStr)
            );
        }
    }

    private static class ParamException extends Exception {
        ParamException(String message) {
            super(message);
        }
    }
}
