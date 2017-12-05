package net.wizards.etherest.bot.dom;

public class PaymentClaim {
    private String walletId;
    private String paySystem;
    private Double amount;
    private String userName;
    private Long chatId;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public void setPaySystem(String paySystem) {
        this.paySystem = paySystem;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getWalletId() {
        return walletId;
    }

    public String getPaySystem() {
        return paySystem;
    }

    public Double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "PaymentClaim{" +
                ",userName='" + userName + '\'' +
                ",chatId='" + chatId + '\'' +
                ", walletId='" + walletId + '\'' +
                ", paySystem='" + paySystem + '\'' +
                ", amount=" + amount +
                '}';
    }
}
