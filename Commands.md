# Java Custom Runtime Image — Commands & Reference Guide

A guide to building a minimal, optimized JVM runtime image for your application using `jdeps` and `jlink`.

---

## Step 1 — Analyze Module Dependencies with `jdeps`

Run the following command from the **root of the project** to determine which JDK modules your application actually needs:

```bash
jdeps \
  --ignore-missing-deps \
  -q \
  --recursive \
  --multi-release 21 \
  --print-module-deps \
  --class-path target/lib/ \
  target/demo-observability-app-1.0.0.jar
```

### What Each Flag Does

| Flag | Purpose |  
|------|---------|  
| `--ignore-missing-deps` | Prevents failure when encountering non-modularized third-party libraries (common in the Spring ecosystem) |  
| `-q` | Quiet mode — suppresses non-essential warnings |  
| `--recursive` | Scans all dependencies, not just top-level ones |  
| `--multi-release 21` | Tells the tool to look into version-specific folders of multi-release JARs (essential for Java 21) |  
| `--print-module-deps` | Outputs a comma-separated list of required JDK modules |  
| `--class-path target/lib/` | Points to external dependencies so their requirements are analyzed too |  

### Example Output

```
java.base, java.desktop
```

> ⚠️ **Note:** `jdeps` only performs static analysis and may not detect all runtime dependencies. The following additional modules are also required:

```
java.logging, java.naming, java.management, jdk.management.agent,
java.security.jgss, java.instrument, java.sql, jdk.unsupported,
jdk.net, java.scripting, jdk.crypto.ec, java.compiler, java.xml, jdk.zipfs
```

---

## Step 2 — Build the Custom Runtime Image with `jlink`
**❗ ** As we are building an observability app with a complex stack (Redis, Vault, Security, Togglz), jlink module list will keep growing as we hit reflection-based errors.
If your goal is minimum image size without the "whack-a-mole" module errors, consider using the full JRE but switching to a distroless base image:  
<i>Use a pre-optimized small JRE instead of building one manually</i>  

```bash  
FROM gcr.io/distroless/java21-debian12  
COPY target/demo-observability-app-1.0.0.jar /app.jar  
CMD ["/app.jar"]  
```

Run the following command to assemble a lightweight, custom JVM containing only the required modules:

```bash
jlink \
  --add-modules java.base,java.logging,java.naming,java.desktop,java.management,\
jdk.management.agent,java.security.jgss,java.instrument,java.sql,jdk.unsupported,\
jdk.net,java.scripting,jdk.crypto.ec,java.compiler,java.xml,jdk.zipfs \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --output custom-runtime-java21
```

### What Each Flag Does

| Flag | Purpose |  
|------|---------|  
| `--add-modules` | Defines which JDK modules to include in the custom runtime |  
| `--strip-debug` | Removes debug information from the binary, significantly reducing size |  
| `--no-man-pages` | Removes help documentation (useless in production containers) |  
| `--no-header-files` | Removes C header files (useless in production containers) |  
| `--compress=2` | Applies maximum ZIP-based compression to the runtime |  
| `--output` | Sets the output folder name for the new lightweight JVM |  

---

## Module Reference — Why Each Module Is Included

| Module | Purpose / Error It Solves |  
|--------|--------------------------|  
| `java.base` | The foundation — handles core logic like `String` and `ArrayList` |  
| `java.desktop` | Required by Spring Data/Web (PropertyEditors), Hibernate/JPA (bean introspection), and configuration libraries that initialize Bean properties at startup |  
| `java.sql` | Required for JPA, JDBC, and H2 database connectivity |  
| `java.naming` | Essential for Spring's JNDI lookups and database connection pooling |  
| `java.logging` | Core logging support |  
| `java.management` | JVM monitoring and management support |  
| `java.instrument` | Required by Spring Boot's dynamic proxying and AOP |  
| `java.security.jgss` | Kerberos and GSSAPI security support |  
| `jdk.unsupported` | Required by many frameworks that use `sun.misc.Unsafe` internally |  
| `jdk.net` | Solves `NoClassDefFoundError: jdk/net/Sockets`; used for optimized networking |  
| `java.scripting` | Solves the Togglz error; required for the `ScriptEngineActivationStrategy` |  
| `java.compiler` | Solves the `SourceVersion` error; used by libraries performing runtime code analysis |  
| `jdk.crypto.ec` | Critical for SSL/TLS and JWT (`jjwt`); often missed by `jdeps` as a dynamic dependency |  
| `jdk.management.agent` | Allows Spring Boot Actuator and Prometheus to monitor the JVM |  
| `java.xml` | Required for XML processing, common in Spring configurations and libraries |  
| `jdk.zipfs` | Required for ZIP file system handling, used by various libraries for resource management |  

---

## Benefits of a Custom `jlink` Runtime

| Benefit | Detail |  
|---------|--------|  
| **Reduced Attack Surface** | Removing unused modules (e.g., Swing, CORBA) eliminates potential security vulnerabilities |  
| **Drastic Size Reduction** | A standard JDK 21 is ~500 MB; your custom runtime will likely be **40–100 MB** |  
| **Improved Startup** | Fewer classes to index and load means faster cold starts in container environments |  
| **Environment Predictability** | Bundling the exact JVM your code needs ensures consistent behaviour from dev to production |  

## Why jpackage is still not the answer for Docker
jpackage is designed to bundle an application for installation on an OS (producing an .exe, .rpm, or .deb).  
It actually adds overhead (metadata, installers, and a specific directory structure) that increases the image size.  
The minimum image size is achieved by taking the custom-runtime (from jlink) and placing it into a "Distroless" or "Alpine" container.  

## Build the Docker image
To build the Docker image using the multi-stage Dockerfile we just architected, you should execute the command from the root directory of your project  
(where the Dockerfile, pom.xml, and vault folder are located). 
 
```bash  
docker build -t demo-observability-app:v1 .
```
**Breakdown of the Command**  
**docker build:** The core command to build an image from a Dockerfile.  
**-t demo-observability-app:v1:** The "tag" flag that assigns a name (demo-observability-app) and a version (v1) to your image.  
**.:** The Context. This tells Docker to look for the Dockerfile and the files to be copied (like your src and vault folders) in the current directory.  


After the Build: The Run CommandOnce the build finishes, you can verify the image size is minimal and the resource limits are applied by running:  

```bash  
  docker run -d \
  --name observability-svc \
  --cpus="2.0" \
  --memory="1024m" \
  --memory-reservation="512m" \
  -p 8080:8080 \
  demo-observability-app:v1
  ```
Memory Optimization: The JVM will initialize at 512 MB and can grow up to 1 GB based on the -XX percentages set in the Dockerfile.  
CPU Optimization: The 2 CPUs allow the ParallelGC we configured to handle the high-throughput requirements of your observability stack effectively.

## To stop docker containers and remove their volumes from the system to free the memory
```bash
docker compose down
docker volume prune -f
```