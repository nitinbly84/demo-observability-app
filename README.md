It is document about what features can be explored & how while running this code.

# **1. Togglz Feature applicable for single instance**
1.  **To access Togglz dashboard:**
    http://localhost:8080/togglz-console/index
2.  **Add a variable for the feature you want to toggle:**
    Update the `com.applicationPOC.togglzFeature.Features` class.
3.  **Add the method for your service:**
    Preferably in `com.applicationPOC.service.FeatureService` (though you can have your service anywhere).
4.  **Have a relevant controller method:**
    Currently located in `com.applicationPOC.controller.PublicController`.
5.  **Check the feature status:**
    Access via `/feature/{feature}`.

**Expected Output:**
* **If enabled:** `Feature 1 is available`
* **If disabled:** `Feature 1 is not available`

**Try using Redis:**
execute `docker-compose.yml' to start Redis and redis-insight to look into Redis data  
Then you can delete the keys in Redis which will reset the togglz keys  
Access redis-insight through `http://localhost:5540/`  
`Check the recommended settings and also check the terms & conditions switch, and then hit the “Submit” button.`    
RedisInsight will launch in your browser. It may ask you to add a Redis database. Just enter and then hit Add Database:  
Host: **redis**  
Port: **6379**
