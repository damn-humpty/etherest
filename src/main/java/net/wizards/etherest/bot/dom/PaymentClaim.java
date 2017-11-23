package net.wizards.etherest.bot.dom;

public class PaymentClaim {
    private String walletId;
    private String paySystem;
    private Double amount;

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public void setPaySystem(String paySystem) {
        this.paySystem = paySystem;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
