package pl.parser.nbp;

import java.util.Date;

/**
 * Wyjątek rzucany w przypadku gdy podano daty w złym porządku.
 */
public class DatesInWrongOrderException extends Exception {
    public DatesInWrongOrderException(Date startDate, Date endDate) {
        super("Zła kolejność dat: " + startDate + " nie jest przed " + endDate);
    }
}
