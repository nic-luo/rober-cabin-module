package group.rober.sql.parser;


import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.statement.*;
import com.alibaba.druid.sql.parser.SQLParserUtils;
import com.alibaba.druid.sql.parser.SQLStatementParser;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;
import com.alibaba.druid.sql.visitor.SchemaStatVisitor;
import group.rober.runtime.kit.StringKit;
import group.rober.runtime.lang.PairBond;
import group.rober.sql.dialect.SqlDialectType;

import java.util.*;

/**
 * SQL语法解析器
 * 主要依托druid进行语法解析，同时，为了屏蔽应用层对druid的直接依赖，统一放到这个类中来处理，以降低应用对某个特定技术的直接依赖
 */
public class SqlSelectParser extends SqlParser{

    private List<SQLSelectItem> selectItems;
    private String select;
    private List<String> columns;
    private String from;
    private Set<String> fromTableAlias;
    private String where;
    private String order;
    private List<PairBond<String,String>> orderItems = new ArrayList<>();

    public SqlSelectParser() {
    }

    public SqlSelectParser(String sql, SqlDialectType sqlDialectType) {
        this.sql = sql;
        this.sqlDialectType = sqlDialectType;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public SqlSelectParser parse(){
        String dbType = getDbType(sqlDialectType);

        StringBuffer fromBuffer = new StringBuffer();
        StringBuffer whereBuffer = new StringBuffer();
        StringBuffer orderBuffer = new StringBuffer();
        // parser得到AST
        SQLStatementParser parser = SQLParserUtils.createSQLStatementParser(sql, dbType);
        List<SQLStatement> stmtList = parser.parseStatementList(); //

        // 将AST通过visitor输出
//        SchemaStatVisitor fromVisitor = SQLUtils.createSchemaStatVisitor(dbType);
        SQLASTOutputVisitor fromVisitor = SQLUtils.createFormatOutputVisitor(fromBuffer, stmtList, dbType);
        SQLASTOutputVisitor whereVisitor = SQLUtils.createFormatOutputVisitor(whereBuffer, stmtList, dbType);
        SQLASTOutputVisitor orderVisitor = SQLUtils.createFormatOutputVisitor(orderBuffer, stmtList, dbType);

        for (SQLStatement stmt : stmtList) {
            if(stmt instanceof SQLCommentStatement)continue;    //忽略注释

            if(stmt instanceof SQLSelectStatement){

                SQLSelectStatement sstmt = (SQLSelectStatement)stmt;
                SQLSelect sqlselect = sstmt.getSelect();
                SQLSelectQueryBlock query = (SQLSelectQueryBlock)sqlselect.getQuery();

                this.selectItems = query.getSelectList();
                this.columns = new ArrayList<String>();
                this.fromTableAlias = new HashSet<String>();

//                stmt.accept(visitor);
                query.getFrom().accept(fromVisitor);
                if(query.getWhere()!=null){
                    query.getWhere().accept(whereVisitor);
                }

                if(query.getOrderBy() != null){
                    query.getOrderBy().accept(orderVisitor);
                }

                SQLOrderBy sqlOrderBy = query.getOrderBy();
                if(sqlOrderBy != null){
                    List<SQLSelectOrderByItem> orderByItems = sqlOrderBy.getItems();
                    for(SQLSelectOrderByItem orderByItem: orderByItems){
                        orderItems.add(new PairBond<String,String>(orderByItem.getExpr().toString(),orderByItem.getType().name));
//                        System.out.println(orderByItem.getExpr()+","+orderByItem.getType()+","+orderByItem.getCollate());
//                        SQLIdentifierExpr expr = new SQLIdentifierExpr();
//                        expr.setName("CREATE_TIME_XXX");
//                        orderByItem.setExpr(expr);
//                        System.out.println(orderByItem.getExpr().getClass().getName());
                    }
                }

                searchAliasInBranch(query.getFrom());   //从FROM子句中解析出第一层直接使用的数据表

                StringBuffer sbSelect = new StringBuffer();
                for(SQLSelectItem selectItem : selectItems){
                    String itemClause = SQLUtils.toSQLString(selectItem, dbType, new SQLUtils.FormatOption(upperCase,false));
                    sbSelect.append(",").append(itemClause);
                    columns.add(itemClause);
                }
                if(sbSelect.length()>0){
                    select = sbSelect.substring(1);
                }


                this.from = fromBuffer.toString();
                this.where = whereBuffer.toString();
                this.order = orderBuffer.toString();
                break;      //只取一句SQL,多的不要
            }
        }


        return this;
    }


    private void fillAliasOrName(String alias,String expr){
        if(StringKit.isNotBlank(alias)){
            fromTableAlias.add(alias);
        }else if(StringKit.isNotBlank(expr)){
            fromTableAlias.add(expr);
        }
    }
    /**
     * 搜索FROM子句上的表别名部分，主要用来处理select * 的情况下，多表需要每个表 TAB1.*,TAB2.*,在oracle数据库下才能作效率最高的分页处理
     */
    private void searchAliasInBranch(SQLTableSource node){
        if(node==null)return;
        if(StringKit.isNotBlank(node.getAlias())){
            fromTableAlias.add(node.getAlias());
        }else if(node instanceof SQLExprTableSource){
            SQLExprTableSource exprLeft = (SQLExprTableSource)node;
            String alias = exprLeft.getAlias();
            String expr = exprLeft.getExpr().toString();
            fillAliasOrName(alias,expr);

        }else if(node instanceof SQLSubqueryTableSource){
            SQLSubqueryTableSource subSelect = (SQLSubqueryTableSource)node;
            String alias = subSelect.getAlias();
            String subSql = subSelect.getSelect().getQuery().toString();
            subSql = SQLUtils.format(subSql,getDbType(sqlDialectType),new SQLUtils.FormatOption(upperCase,false));
            String expr = "("+subSql+")";//子查询放括号中
            fillAliasOrName(alias,expr);
        }
        if(node instanceof SQLJoinTableSource){
            SQLTableSource left = ((SQLJoinTableSource)node).getLeft();
            SQLTableSource right = ((SQLJoinTableSource) node).getRight();
            searchAliasInBranch(left);
            searchAliasInBranch(right);
        }

    }

    public String getSelect() {
        return select;
    }

    public List<String> getColumns(){
        return this.columns;
    }

    public String getFrom() {
        return from;
    }

    public Set<String> getFromTableAlias() {
        return fromTableAlias;
    }

    public String getWhere() {
        return where;
    }

    public String getOrder() {
        return order;
    }

    public List<PairBond<String, String>> getOrderItems() {
        return orderItems;
    }

    private SchemaStatVisitor getSchemaStatVisitor(SqlDialectType dialectType){
        return visitorMap.get(dialectType);
    }

    public String format(){
        return format(sql());
    }

    public String sql(){
        StringBuffer sqlBuffer = new StringBuffer();
        sqlBuffer.append(" SELECT ").append(select);
        sqlBuffer.append(" FROM ").append(from);
        if(StringKit.isNotBlank(where)){
            sqlBuffer.append(" WHERE ").append(where);
        }
        if(StringKit.isNotBlank(order)){
            sqlBuffer.append(" ").append(order);
        }
        return sqlBuffer.toString();
    }

    public String format(String sqlText){
        String dbType = getDbType(sqlDialectType);
        String fmtSql = SQLUtils.format(sqlText,dbType,new SQLUtils.FormatOption(upperCase,prettyFormat));
        return fmtSql;
    }

}
