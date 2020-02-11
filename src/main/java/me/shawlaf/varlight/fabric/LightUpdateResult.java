package me.shawlaf.varlight.fabric;

public enum LightUpdateResult {

    CANNOT_MODIFY, ILLEGAL_BLOCK, ZERO_REACHED, FIFTEEN_REACHED, SUCCESS;

    public boolean isSuccess() {
        return this == SUCCESS;
    }

}
