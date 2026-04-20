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
