/*
 * Accommodation.java
 *
 * Created on 6 March 2006, 11:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.ucla.cs.cs144;

/**
 *
 * @author John
 */
public class Category {

    /** Creates a new instance of Accommodation */
    public Category() {
    }

    /** Creates a new instance of Accommodation */
    public Category(String itemId,
                 String name) {
        this.itemId = itemId;
        this.name = name;
    }

    /**
     * Holds value of item name.
     */
    private String name;

    /**
     * Getter for item title.
     * @return Value of item title.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Setter for item title.
     * @param title New value of item title.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Holds value of item id.
     */
    private String itemId;

    /**
     * Getter for item id.
     * @return Value of item id.
     */
    public String getItemId() {
        return this.itemId;
    }

    /**
     * Setter for item id.
     * @param id New value of item id.
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String toString() {
        return "Category "
               + getItemId()
               +": "
               + getName();
    }
}
