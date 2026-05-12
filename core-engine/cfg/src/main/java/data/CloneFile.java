package data;
import static core.path.MarkedPath.markOneStatement;
public class CloneFile {
public static int sampleMethod(int a, int b, int c)
{
if (((a > 10) && markOneStatement("a > 10", true, false)) || markOneStatement("a > 10", false, true))
{
{
if (((c < 20) && markOneStatement("c < 20", true, false)) || markOneStatement("c < 20", false, true))
{
{
markOneStatement("int i=0", false, false);
for (int i=0; ((i < b) && markOneStatement("i < b", true, false)) || markOneStatement("i < b", false, true); markOneStatement("i=i + 1", false, false),
i=i + 1) {
{
markOneStatement("System.out.println(\"hello\");\n", false, false);
System.out.println("hello");
}
}
if (((b < -5) && markOneStatement("b < -5", true, false)) || markOneStatement("b < -5", false, true))
{
{
markOneStatement("System.out.println(\"bye\");\n", false, false);
System.out.println("bye");
}
}
}
}
else {
{
markOneStatement("return a + b + c;\n", false, false);
return a + b + c;
}
}
}
}
else {
{
if (((a + b - c == 10) && markOneStatement("a + b - c == 10", true, false)) || markOneStatement("a + b - c == 10", false, true))
{
{
markOneStatement("System.out.println(\"hehe\");\n", false, false);
System.out.println("hehe");
}
}
else {
if (((a > b) && markOneStatement("a > b", true, false)) || markOneStatement("a > b", false, true))
{
{
markOneStatement("System.out.println(\"hoo\");\n", false, false);
System.out.println("hoo");
}
}
}
}
}
markOneStatement("return 5;\n", false, false);
return 5;
}
}
