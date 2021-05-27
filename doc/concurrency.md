# Concurrency design

## Initial design: futures

As for parallel processing, we initially implemented a solution based on `FixedThreadPool`s and `CompletableFuture`s, which is a simple and comprehensible framework. As usual, futures allow for two distinct types of programming: 

- `then`-based programming, where callback functions can be specified, and several callbacks and futures can be chained to do a certain sequence of actions. Several futures can also be chained if the program is required to perform a sequence of asynchronous tasks which depend on each other.
- The synchronous-looking way, which consists of replacing a few return types with futures, and when that future is effectively needed the program can call `get()` in Java (or `await` in JavaScript).

The second option has one major disadvantage: if the futures are being run with a `FixedThreadPool`, when a future calls `get()` on another future the CPU deduces the calling thread is blocked and switches context to do something else, but the resources required to maintain a thread are still being used. This means that, although the process might seem to be using many threads, a large portion of those might be blocked. Thus thread pool usually needs the number of threads it has to be at least one order of magnitude above the maximum parallelism level of the machine (if the CPU can run 8 threads, a good value for the number of threads in the pool is 20-100).

The first option is usually the best, because chaining promises allows, for instance, for the executor to realise that, in a situation where we have `aFuture.then(doSomething)`, the same thread that ran `aFuture` can also run `doSomething` without introducing much overhead and avoiding `get()` calls (which reduces the number of futures that are blocked waiting for other futures).

We tried to frequently return futures so we could chain them as much as possible, but still it was not enough, as some protocols needed to create several instances of lower-level protocols (each instance corresponding to a future) and then wait for all of them to complete. Aside from that, the implementation became rather messy.

## Currrent solution: fork-join pools

When processing incoming messages with `Peer.ServerSocketHandler`, we decided to stick with a `FixedThreadPool` solution with 20 threads. However, for everything else (including protocols on the initiator side and message processing on the receiving side) we opted for a simpler solution: `ForkJoinPool` and the associated classes.

This set of classes allows the user to run *tasks*, which are small (but not too minuscule) pieces of code that produce some effect (`RecursiveAction`) or result (`RecursiveTask`). This framework is specially designed with recursive tasks in mind, since a task can internally create other, smaller tasks. A task can either be run with `invoke()`, which causes its code to run in the current thread as if it was a regular `Supplier`, or it can also run in parallel if the main task calls `fork()` for the task to start running in parallel, and later `join()` to wait for their completion (and if subtasks have end-products they can be retrieved using `get`; this function can also be called even if the task was not forked before, as it will automatically fork); a collection of tasks can be collectively forked by calling `invokeAll` from inside the main task, and then each task can be joined individually.

A `ForkJoinPool` keeps, for each task, a queue of subtasks it summoned, and each task is assigned a thread. If a task blocks waiting for a subtask, the thread $a$ starts running the subtask; if another thread $b$ does not have anything to do and notices $a$ is struggling to keep up with a large number of queued tasks, $b$ *steals* work from $a$ and starts running some of those subtasks itself, so that when $a$ joins the subtasks, some of them have already been processed by $b$ and thus do not require $a$ to block nor process them. This is called *work stealing* .

There are two main advantages with this approach:

- `ForkJoinPool` takes care of creating new threads and deleting threads that have not been active for a while (usually 1 minute), which means we no longer have to estimate how many threads are enough.
    - `ForkJoinPool` also notices when a task blocks waiting for a subtask to be completed, and that thread starts processing that task, instead of the future approach where the calling thread blocks and another thread is allocated to the subtask/subfuture.
    - Additionally, the class `ForkJoinPool.ManagedBlocker` can be implemented to tell the pool that that specific task blocks waiting for something, and as such the pool must create a new thread to replace the thread that will block. This is useful when writing to files; even if we are using NIO classes for reading and writing, we need to convert the `CompletableFuture`s it uses into tasks, which we do by calling `get()` on those futures, thus making them blocking. This has two advantages: the reading is still non-busy (because the blocking task is blocked and the CPU knows that) and we can integrate the NIO classes into our `ForkJoinPool` framework.

- Code can be run synchronously (`invoke`) or in parallel (`fork`/`invokeAll` and `join`/`get`) at the programmer's discretion.

This gives us more control over the whole process, as well as keeping the code tidier. We additionally observed a $15\%$ decrease in execution time of automated tests, which is explained by less thread blocking and better thread management (as threading is mostly managed by the `ForkJoinPool`).

The framework already creates a process-level static `ForkJoinPool`, which means we do not need to worry with creating a pool of our own (although we could).
