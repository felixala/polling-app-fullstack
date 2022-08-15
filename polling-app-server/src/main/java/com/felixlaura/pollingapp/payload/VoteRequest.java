package com.felixlaura.pollingapp.payload;

import javax.validation.constraints.NotNull;

public class VoteRequest {

    @NotNull
    private long choiceId;

    public long getChoiceId() {
        return choiceId;
    }

    public void setChoiceId(long choiceId) {
        this.choiceId = choiceId;
    }
}
