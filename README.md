### Running the code

The project uses the Gradle Application plugin which means that running is as simple as

    ./gradlew run

This will spawn the external process in vendor/ (linux-x64), start processing records and 
provide an HTTP endpoint at :8080.

The Application plugin can also be used to generate a standalone zip by running `gradle distZip`.
Unzipping this file and running `bin/bpbackend` will, again, run the app.

The code is in src/main/java/async (main is in Main.java).
An alternative blocking version is in src/main/java/blocking.

### Possible improvements

1. It probably makes sense to periodically save the collected statistics to durable storage 
   in order not to lose the collected data if the process or machine die.

2. The current design *briefly* blocks the StatProcessor handlers when the HTTP endpoint is accessed.
   It is probably better to call pause() on StatsProcessor, and avoid blocking the event loop.
   Alternatively, if consistency is not crucial, we remove the synchronized{} mutex. Throughput
   will be higher but the reported counts may not match exactly any point in time.
   In this case the HashMap-s will have to replaced with a ConcurrentHashMap to provide thread-safety.

3. Vert.x runs a single thread per processor. As the number of processors gets higher, performance
   will suffer due to contention when accessing the histogram maps.
   An alternative implementation would hold a map per processor, thus avoiding contension between
   the different CPUs until the HTTP endpoint is accessed and the maps must be combined
   before returning a response.


### Assumptions

1. Records are newline terminated. Json objects will not contain newlines.
2. Input is UTF-8 encoded. Invalid UTF-8 codepoints are discarded.
3. The source (external process) cannot handle back pressure. I.e., we never block the external 
   process standard output and prefer to drop events.
4. A single machine can handle the load. If not, events will be dropped.
5. The sizes of the histograms (number of types and number of distinct words) are small enough
   to make serving serialized JSON reasonable.
6. The HTTP endpoint should serve a consistent view (i.e., the histograms should be "sampled" atomically)


### Final notes

1. The simplest solution satisfying the requirements is, in my opinion, the blocking version.
   Since the consumers are cpu-bound and do not peform any I/O, one can simply spawn multiple
   blocking consumers (as many as the number of CPUs. More will just increase contention).

2. I wasn't familiar with Vert.x when starting this project (still not familiar, I guess) 
   and failed to find a way to convert the standard output of the external process to a event
   stream without creating another Java thread.

Thanks for reading,
- N.
