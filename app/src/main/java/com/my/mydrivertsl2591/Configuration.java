package com.my.mydrivertsl2591;

public class Configuration {
    public float getRez() {
        return rez;
    }

    public void setRez(float rez) {
        this.rez = rez;
    }

    private float rez;
    public Configuration(){}
    public Configuration(Float rez){
        this.rez = rez;
    }


}