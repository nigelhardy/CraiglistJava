package craigslist;

import java.util.Comparator;

public class ValueSorter implements Comparator<Listing> 
{
    public int compare(Listing o1, Listing o2) {
        return o1.getValue().compareTo(o2.getValue());
    }
}