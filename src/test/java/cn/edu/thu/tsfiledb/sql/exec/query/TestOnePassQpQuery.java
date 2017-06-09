package cn.edu.thu.tsfiledb.sql.exec.query;

import cn.edu.thu.tsfile.common.constant.SystemConstant;
import cn.edu.thu.tsfile.timeseries.read.qp.Path;
import cn.edu.thu.tsfile.timeseries.read.query.QueryDataSet;
import cn.edu.thu.tsfile.timeseries.utils.StringContainer;
import cn.edu.thu.tsfiledb.qp.exception.QueryProcessorException;
import cn.edu.thu.tsfiledb.qp.logical.operator.RootOperator;
import cn.edu.thu.tsfiledb.sql.exec.TSqlParserV2;
import cn.edu.thu.tsfiledb.sql.exec.utils.MemIntQpExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


/**
 * test query operation
 * 
 * @author kangrong
 *
 */
@RunWith(Parameterized.class)
public class TestOnePassQpQuery {
    private static final Logger LOG = LoggerFactory.getLogger(TestOnePassQpQuery.class);
    private MemIntQpExecutor exec = new MemIntQpExecutor();
    private Path path1;
    private Path path2;

    private final String inputSQL;
    private final String[] expectRet;

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays
                .asList(new Object[][] {
                        {
                                "select d1.s1 from root.laptop where root.laptop.d1.s1 < 100",
                                new String[] {"20, <root.laptop.d1.s1,21> ",
                                        "40, <root.laptop.d1.s1,41> ",
                                        "60, <root.laptop.d1.s1,61> ",
                                        "80, <root.laptop.d1.s1,81> "}},
                        {
                                "select root.laptop.d1.s2 where root.laptop.d1.s2 < 200",
                                new String[] {"50, <root.laptop.d1.s2,52> ",
                                        "100, <root.laptop.d1.s2,102> ",
                                        "150, <root.laptop.d1.s2,152> "}}});
    }

    public TestOnePassQpQuery(String sql, String[] ret) {
        inputSQL = sql;
        this.expectRet = ret;
    }

    @Before
    public void before() {
        path1 =
                new Path(new StringContainer(new String[] {"root", "laptop", "d1", "s1"},
                        SystemConstant.PATH_SEPARATOR));
        path2 =
                new Path(new StringContainer(new String[] {"root", "laptop", "d1", "s2"},
                        SystemConstant.PATH_SEPARATOR));
        for (int i = 1; i <= 10; i++) {
            exec.insert(path1, i * 20, Integer.toString(i * 20 + 1));
            exec.insert(path2, i * 50, Integer.toString(i * 50 + 2));
        }
    }

    @Test
    public void testQueryBasic() throws QueryProcessorException {
        TSqlParserV2 parser = new TSqlParserV2();
        RootOperator root = parser.parseSQLToOperator(inputSQL);
        if (!root.isQuery())
            fail();
        Iterator<QueryDataSet> iter = parser.query(root, exec);
        System.out.println("query result:\n");
        int i = 0;
        while (iter.hasNext()) {
            QueryDataSet set = iter.next();
            while (set.hasNextRecord()) {
                if (i == expectRet.length)
                    fail();
                String actual = set.getNextRecord().toString();
                System.out.println(actual);
                assertEquals(expectRet[i++], actual);
            }
        }
        assertEquals(expectRet.length, i);
    }

    /**
     * 20, <device_1.sensor_1,21> <device_1.sensor_2,null> <br>
     * 50, <device_1.sensor_1,null> <device_1.sensor_2,52> <br>
     * 100, <device_1.sensor_1,101> <device_1.sensor_2,102>
     */
}
