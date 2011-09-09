package edisdataclient

import java.text.SimpleDateFormat

/**
 * User: carlos
 * Date: 9/7/11
 * Time: 9:54 PM
 */
@Category(String)
class StringToDateCategory {

  static originalMethod = String.metaClass.getMetaMethod('asType', [Class] as Object[])

  /**
   * Allows String conversion to a java.util.Date() object.
   *
   * @return
   */
  /*
   * this isn't as clean as overriding asType() - but apparently a Category
   * cannot call superclass functionality.  So an override of asType() wouldn't
   * be able to fallback on super.asType() if the Class arg wasn't a date.  LAME!
   */
  def asType(Class type) {
    if (Date.class.equals(type)) {
      new SimpleDateFormat('yyyy-MM-dd hh:mm:ss').parse(this)
    } else {
      originalMethod.invoke(this, [type] as Object[])
    }
  }
}
