package practice2.suite;

import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Slow tests")
@SelectPackages("practice2")
@IncludeTags("slow")
public class SlowSuite {
}
