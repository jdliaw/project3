/*
 * Accommodation.java
 *
 * Created on 6 March 2006, 11:58
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package lucene.demo.business;

/**
 *
 * @author John
 */
public class Item {

    /** Creates a new instance of Accommodation */
    public Item() {
    }

    /** Creates a new instance of Accommodation */
    public Item(String id,
                 String name,
                 String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
    private String id;

    /**
     * Getter for item id.
     * @return Value of item id.
     */
    public String getId() {
        return this.id;
    }

    /**
     * Setter for item id.
     * @param id New value of item id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Holds value of item description.
     */
    private String description;

    /**
     * Getter for item details.
     * @return Value of item details.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Setter for item details.
     * @param details New value of item details.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    public String toString() {
        return "Item "
               + getId()
               +": "
               + getName()
               +" ("
               + getDescription()
               +")";
    }
}
