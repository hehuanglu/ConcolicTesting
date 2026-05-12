package core.testGeneration.NTDTestGeneration.NTDPairwiseTesting;

public class Pair {
    private Object value1;
    private int placeOfParameter1; // là parameter có thứ tự bao nhiêu trong hàm đó (vd: 0, 1, 2, 3, ...) (Bắt đầu từ 0)
    private Object value2;
    private int placeOfParameter2;

    public Pair(Object value1, int placeOfParameter1, Object value2, int placeOfParameter2) {
        this.value1 = value1;
        this.placeOfParameter1 = placeOfParameter1;
        this.value2 = value2;
        this.placeOfParameter2 = placeOfParameter2;
    }

    public Object getValue1() {
        return value1;
    }

    public int getPlaceOfParameter1() {
        return placeOfParameter1;
    }

    public Object getValue2() {
        return value2;
    }

    public int getPlaceOfParameter2() {
        return placeOfParameter2;
    }

    @Override
    public boolean equals(Object another) {
        if (another instanceof Pair) {
            Pair anotherPair = (Pair) another;
            return this.straitCheck(anotherPair) || this.crossCheck(anotherPair);
        }
        return false;
    }

    private boolean straitCheck(Pair another) {
        return (value1.equals(another.value1))
                && (value2.equals(another.value2))
                && (placeOfParameter1 == another.placeOfParameter1)
                && (placeOfParameter2 == another.placeOfParameter2);
    }

    private boolean crossCheck(Pair another) {
        return (value1.equals(another.value2))
                && (value2.equals(another.value1))
                && (placeOfParameter1 == another.placeOfParameter2)
                && (placeOfParameter2 == another.placeOfParameter1);
    }

    public boolean equalsPlace1AndPlace2AndValue2(Object another) {
        if (another instanceof Pair) {
            Pair anotherPair = (Pair) another;
            return (value2.equals(anotherPair.value2))
                    && (placeOfParameter1 == anotherPair.placeOfParameter1)
                    && (placeOfParameter2 == anotherPair.placeOfParameter2);
        }
        return false;
    }
}
