package com.ctg.coptok.data.models;

public class Item {

    public int id;
    public String name;
    public String image;
    public int price;
    public String value;
    public int minimum;

    public static class CartItem {

        public int id;
        public int quantity;
    }
}
