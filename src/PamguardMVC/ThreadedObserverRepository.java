package PamguardMVC;

import java.util.ArrayList;

/**
 * Used in run extract mode to terminate all threaded observers when loading a new psfx
 * @author Sam
 *
 */
public class ThreadedObserverRepository {
	
	private static ThreadedObserverRepository instance;
	
	private ArrayList<ThreadedObserver> observerList;
	
	private ThreadedObserverRepository() {
		observerList = new ArrayList<ThreadedObserver>();
	}
	
	public static ThreadedObserverRepository create() {
		if(instance==null) {
			instance = new ThreadedObserverRepository();
		}
		return instance;
	}
	
	public static ThreadedObserverRepository getInstance() {
		return instance;
	}
	
	public void addObserver(ThreadedObserver nextObserver) {
		observerList.add(nextObserver);
	}
	
	public void stopAndDeleteAllObservers() {
		while(!observerList.isEmpty()) {
			observerList.remove(0).terminateThread();
		}
	}

}
