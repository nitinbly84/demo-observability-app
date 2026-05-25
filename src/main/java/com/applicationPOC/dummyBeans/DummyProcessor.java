package com.applicationPOC.dummyBeans;

import java.util.Map;

import org.springframework.stereotype.Service;

// This class is a service that processes dummy beans to show how Spring can provide the map of all concrete classes objects based on interface used. 
// It takes a map of dummy beans as a constructor argument and provides a method to process the beans based on their names. 
//The process method filters the keys of the dummyBeans map to find a key that contains the specified name and returns the corresponding ParentDummy object. 
// If no matching key is found, it returns null.
@Service
public class DummyProcessor {
	
	private final Map<String, ParentDummy> dummyBeans;
	
	// The constructor takes a map of dummy beans, which is automatically injected by Spring based on the available beans 
	// that implement the ParentDummy interface.
	public DummyProcessor(Map<String, ParentDummy> dummyBeans) {
		this.dummyBeans = dummyBeans;
	}
	
	public ParentDummy process(String name) {
		System.out.println(dummyBeans.toString());
		return dummyBeans.keySet().stream()
		.filter(key -> key.contains(name))
		.map(dummyBeans::get)
		.findFirst()
		.orElse(null);
	}

}
