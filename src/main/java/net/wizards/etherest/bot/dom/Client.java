package net.wizards.etherest.bot.dom;

import com.google.gson.Gson;
import com.pengrad.telegrambot.model.User;
import net.wizards.etherest.Config;
import net.wizards.etherest.util.Misc;

public class Client {
    private int id;
    private String firstName;
    private String lastName;
    private String userName;
    private String langCode;
    private String walletId;

    public Client() {
    }

    public static Client from(User user) {
        return new Builder()
                .setId(user.id())
                .setFirstName(user.firstName())
                .setLastName(user.lastName())
                .setUserName(user.username())
                .setLangCode(Misc.nvl(user.languageCode(), Config.get().getDefaultLang()))
                .build();
    }

    private Client(int id, String firstName, String lastName, String userName, String langCode, String walletId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userName = userName;
        this.langCode = langCode;
        this.walletId = walletId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Client client = (Client) o;

        return id == client.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", langCode='" + langCode + '\'' +
                ", walletId='" + walletId + '\'' +
                '}';
    }

    public void setLangCode(String langCode) {
        this.langCode = langCode;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public int getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUserName() {
        return userName;
    }

    public String getLangCode() {
        return langCode;
    }

    public String getWalletId() {
        return walletId;
    }

    public static class Builder {
        private int id;
        private String firstName;
        private String lastName;
        private String userName;
        private String langCode;
        private String walletId;

        public Builder setId(int id) {
            this.id = id;
            return this;
        }

        public Builder setFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder setLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder setLangCode(String langCode) {
            this.langCode = langCode;
            return this;
        }

        public Builder setWalletId(String walletId) {
            this.walletId = walletId;
            return this;
        }

        public Client build() {
            return new Client(id, firstName, lastName, userName, langCode, walletId);
        }
    }
}
