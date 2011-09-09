package edisdataclient

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.assertEquals
import java.text.SimpleDateFormat
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

/**
 * User: carlos
 * Date: 9/7/11
 * Time: 3:35 PM
 */
class StringToDateCategoryTest {
  
  def expectedFormat

  @Before
  void setUp() {
    expectedFormat = new SimpleDateFormat('yyyy-MM-dd hh:mm:ss')
  }

  @Test(expected=GroovyCastException)
  void doesntApplyUnlessYouUseIt() {
    "this can't be converted to a date" as Date
  }

  @Test
  void canConvertGStringToDate() {
    def toConvert = "2006-01-12 22:19:07"
    def expectedDate = expectedFormat.parse(toConvert)
    use(StringToDateCategory) {
      assertEquals expectedDate, toConvert as Date
    }
  }

  @Test
  void canConvertStringToDate() {
    def toConvert = '2006-01-12 22:19:07'
    def expectedDate = expectedFormat.parse(toConvert)
    use(StringToDateCategory) {
      assertEquals expectedDate, toConvert as Date
    }
  }

  @Test
  void useDoesntPreventStandardCasts() {
    use(StringToDateCategory) {
      assertEquals 12, '12' as int
    }
  }

}

