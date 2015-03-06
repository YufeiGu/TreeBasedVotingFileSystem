package fileserver;

public class TestThread {  
	 public synchronized void execute() { // synchronized,未修饰  
	  for (int i = 0; i < 1000; i++) {  
	   System.out.println(i);  
	  }  
	 }  
	}  
	  
	class TestThread2 implements Runnable {  
	 TestThread test = null;  
	  
	 public TestThread2(TestThread pTest) { // 对象有外部引入，这样保证是同一个对象  
	  test = pTest;  
	 }  
	   
	 public void run() {  
	  test.execute();  
	 }  
	   
	 public static void main(String[] args)  
	 {  
	  TestThread test=new TestThread();  
	  Runnable runabble=new TestThread2(test);  
	  Thread a=new Thread(runabble,"A");                  
	  a.start();  
	  Thread b=new Thread(runabble,"B");  
	  b.start();  
	 }  
	} 