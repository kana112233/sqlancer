package sqlancer.cockroachdb.oracle.tlp;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.DatabaseProvider;
import sqlancer.Randomly;
import sqlancer.TestOracle;
import sqlancer.cockroachdb.CockroachDBProvider.CockroachDBGlobalState;
import sqlancer.cockroachdb.CockroachDBSchema.CockroachDBDataType;
import sqlancer.cockroachdb.CockroachDBVisitor;
import sqlancer.cockroachdb.ast.CockroachDBExpression;
import sqlancer.cockroachdb.ast.CockroachDBNotOperation;
import sqlancer.cockroachdb.ast.CockroachDBUnaryPostfixOperation;
import sqlancer.cockroachdb.ast.CockroachDBUnaryPostfixOperation.CockroachDBUnaryPostfixOperator;

public class CockroachDBTLPWhereOracle extends CockroachDBTLPBase {

    public CockroachDBTLPWhereOracle(CockroachDBGlobalState state) {
        super(state);
        errors.add("GROUP BY term out of range");
    }

    @Override
    public void check() throws SQLException {
        super.check();
        String originalQueryString = CockroachDBVisitor.asString(select);

        List<String> resultSet = DatabaseProvider.getResultSetFirstColumnAsString(originalQueryString, errors,
                state.getConnection(), state);

        boolean allowOrderBy = Randomly.getBoolean();
        if (allowOrderBy) {
            select.setOrderByExpressions(gen.getOrderingTerms());
        }
        CockroachDBExpression predicate = gen.generateExpression(CockroachDBDataType.BOOL.get());
        select.setWhereClause(predicate);
        String firstQueryString = CockroachDBVisitor.asString(select);
        select.setWhereClause(new CockroachDBNotOperation(predicate));
        String secondQueryString = CockroachDBVisitor.asString(select);
        select.setWhereClause(new CockroachDBUnaryPostfixOperation(predicate, CockroachDBUnaryPostfixOperator.IS_NULL));
        String thirdQueryString = CockroachDBVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = TestOracle.getCombinedResultSet(firstQueryString, secondQueryString,
                thirdQueryString, combinedString, !allowOrderBy, state, errors);
        TestOracle.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString, state);
    }
}
