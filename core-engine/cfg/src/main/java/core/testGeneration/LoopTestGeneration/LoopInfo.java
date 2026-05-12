package core.testGeneration.LoopTestGeneration;

import core.cfg.CfgBoolExprNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;

public class LoopInfo {
    private Statement loopStatement; // Cấu trúc vòng lặp (for, while, do-while)
    private Expression conditionExpr;
    private String iteratorVariable; // Tên biến lặp (i, j, ...)
    private int startValue;         // Giá trị khởi tạo
    private int endValue;           // Giá trị kết thúc
    private int stepValue;          // Độ tăng/giảm
    private int maxIterations;      // Số lần lặp tối đa
    private String loopParameter;  // Tham số ảnh hưởng đến vòng lặp (như b trong i<b)
    private String startParameter; // Tham số ảnh hưởng đến giá trị bắt đầu (như a trong i=a)
    private String endParameter;   // Tham số ảnh hưởng đến giá trị kết thúc (như b trong i<b)
    private String stepParameter;  // Tham số ảnh hưởng đến độ tăng/giảm (như c trong i+=c)
    private String loopType;       // Loại vòng lặp (for, while, do-while)

    public LoopInfo() {
        this.stepValue = 1; // Giá trị mặc định
    }

    public boolean isParameterAffectingLoop(String paramName) {
        // Nếu đã xác định được tham số ảnh hưởng
        if (loopParameter != null) {
            return paramName.equals(loopParameter);
        }

        // Kiểm tra thông qua điều kiện
        if (conditionExpr instanceof InfixExpression) {
            InfixExpression infixExpr = (InfixExpression) conditionExpr;

            // Kiểm tra nếu tham số xuất hiện ở vế phải điều kiện
            Expression rightOp = infixExpr.getRightOperand();
            if (rightOp instanceof SimpleName) {
                String rightVarName = ((SimpleName) rightOp).getIdentifier();
                return paramName.equals(rightVarName);
            }
        }
        return false;
    }

    /**
     * Lấy tên tham số ảnh hưởng đến vòng lặp
     */
    public String getLoopParameterName() {
        if (conditionExpr instanceof InfixExpression) {
            InfixExpression infixExpr = (InfixExpression) conditionExpr;
            Expression rightOp = infixExpr.getRightOperand();

            if (rightOp instanceof SimpleName) {
                return ((SimpleName) rightOp).getIdentifier();
            }
        }
        return null;
    }

    public Expression getConditionExpr() {
        return conditionExpr;
    }

    public void setConditionExpr(Expression conditionExpr) {
        this.conditionExpr = conditionExpr;
    }

    public Statement getLoopStatement() {
        return loopStatement;
    }

    public void setLoopStatement(Statement loopStatement) {
        this.loopStatement = loopStatement;
    }

    public String getIteratorVariable() {
        return iteratorVariable;
    }

    public void setIteratorVariable(String iteratorVariable) {
        this.iteratorVariable = iteratorVariable;
    }

    public int getStartValue() {
        return startValue;
    }

    public void setStartValue(int startValue) {
        this.startValue = startValue;
    }

    public int getEndValue() {
        return endValue;
    }

    public void setEndValue(int endValue) {
        this.endValue = endValue;
    }

    public int getStepValue() {
        return stepValue;
    }

    public void setStepValue(int stepValue) {
        this.stepValue = stepValue;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public String getLoopParameter() {
        return loopParameter;
    }

    public void setLoopParameter(String loopParameter) {
        this.loopParameter = loopParameter;
    }

    public String getStartParameter() {
        return startParameter;
    }

    public void setStartParameter(String startParameter) {
        this.startParameter = startParameter;
    }

    public String getEndParameter() {
        return endParameter;
    }

    public void setEndParameter(String endParameter) {
        this.endParameter = endParameter;
    }

    public String getStepParameter() {
        return stepParameter;
    }

    public void setStepParameter(String stepParameter) {
        this.stepParameter = stepParameter;
    }

    public String getLoopType() {
        return loopType;
    }

    public void setLoopType(String loopType) {
        this.loopType = loopType;
    }

    @Override
    public String toString() {
        return "LoopInfo{" +
                "iteratorVariable='" + iteratorVariable + '\'' +
                ", startValue=" + startValue +
                ", endValue=" + endValue +
                ", stepValue=" + stepValue +
                ", maxIterations=" + maxIterations +
                '}';
    }
}