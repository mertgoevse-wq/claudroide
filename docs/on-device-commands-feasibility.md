# Running project commands on Android — feasibility report

Task 105. Written 2026-10-02. Every claim below names its source; anything not
sourced is marked as a project decision or left open.

**Short answer:** a normal third-party Android app cannot run a Gradle build on the
device. Android has the pieces for it — AVF and a Linux development environment —
but the interfaces that reach them are reserved for the platform. What *is* feasible
is running the app's own unit tests in process, without Gradle and without a
separate terminal app. That is the path this project should take.

---

## 1. The options, and what each one actually is

### Option A — Android Virtualization Framework (AVF)

AVF is real and is used for compilation on Android today.

> "As a software-secure enclave, a protected virtual machine (VM) provides a safe
> environment to compile security-sensitive code. […] moving the compilation of
> bootclasspath and system server JARs (triggered by an APEX update) from early boot
> to before reboot."
> — source.android.com/docs/core/virtualization/usecases

So Android compiles code inside a VM in production. But read who does it:

> "The implementation is in the com.android.compos APEX. This component is optional
> and can be included using a makefile."
> — source.android.com/docs/core/virtualization/usecases

`com.android.compos` is a **system** component shipped in an APEX, selected at
build time with a makefile. It is not an API a Play-distributed app calls.

And the interfaces themselves:

> "Java API — The VirtualizationService Java APIs, which are present only on devices
> with AVF support. These APIs are optional and not part of the bootclasspath."
> — source.android.com/docs/core/virtualization

> "AVF is supported only on ARM64 devices."
> — source.android.com/docs/core/virtualization

The A56 is ARM64, so that part fits. But **"optional and not part of the
bootclasspath"** is the decisive sentence: a third-party app cannot compile against
`VirtualizationService`, cannot assume it exists on a user's phone, and would be
building against an interface Google reserves for the platform. Using it would make
the app work on some devices and silently do nothing on others.

**Verdict: not usable for a third-party app. Not a question of effort — the interface
is not ours to call.**

### Option B — Android's Linux development environment

This is the most interesting find, and the most misunderstood.

> "Android has traditionally been the only major operating system that doesn't let
> users develop apps on the platform itself. With the introduction of the Linux
> development environment, we aim to provide a Linux-based development environment
> to Android users who are developers."
> — source.android.com/docs/core/virtualization/usecases

> "The Linux development environment is available on select devices and runs in a non
> protected virtual machine."
> — source.android.com/docs/core/virtualization/usecases

Three limits, all stated in the source:

1. **"on select devices"** — not guaranteed on a Galaxy A56. Task 006 records Android
   15 One UI 7 as a *presumption* for the target device, not a measured fact, and no
   measurement exists yet.
2. **developer options must be enabled** — a manual step by the user, on a consumer
   phone.
3. **it is the Terminal app** — a user-facing terminal, not a service an app drives.

This is Android telling developers: use *this app* for Linux work. It is a good answer
to "how do developers develop on Android" and it is **not** an answer to "how does my
app run a build". There is no documented API to spin up that environment, attach a
stdin, and read a result.

**Verdict: exists, is the right direction, is not addressable by a third-party app.**

### Option C — proot / Termux

Technically works, and is what people actually do today. It needs a separate terminal
app, which task 105 explicitly sets aside ("ohne separate Termux-App"). It is also
slow, and pKVM-backed AVF exists precisely because the emulated path was too slow to
use for boot-path compilation.

**Verdict: out of scope for this task, and the reason the task excludes it is sound.**

### Option D — Cloud / CI

Builds on a remote runner. Real and common. But it means the user's source leaves the
device, which is a decision the user has to make, not a default. It also costs money.

**Verdict: possible, but it is a product decision with privacy and cost
consequences, not a technical fallback. Not decided here.**

---

## 2. What is actually feasible: running the app's own tests in process

This is the finding worth acting on, and it comes from a property this project
already has.

Every policy class in this codebase is **pure Kotlin with no Android imports** —
`AgentRunStore`, `ChangeReview`, `GitChangeListPolicy`, `ProjectBoundaryEnforcer` and
the rest. That was a deliberate constraint from the first tasks so the guarantees
would be testable on a plain JVM. It turns out to also be the thing that makes an
on-device test runner possible:

- No Gradle needed. A test is a class with `@Test` methods; run it by reflection.
- No JDK installation. The classes are already on the device inside the app.
- No separate terminal app. It runs in the app process.
- The Android SDK classes the tests avoid are exactly the ones that would need
  instrumentation.

The honest limit: this runs **the app's own logic tests**, not a Gradle build of an
arbitrary user's project. `:app:testDebugUnitTest` on a developer's machine also
compiles the app and runs lint, assembleDebug and sync_frontmatter — none of which
work this way. A test result from this path is evidence about *this app's rules*, and
must never be presented as "the project builds".

---

## 3. The guard rails this implies

These are already mostly in place, from earlier tasks, and the report records them
rather than inventing new ones:

| Guard | Where it lives |
| :--- | :--- |
| Only allowlisted commands may run | `AgentToolCatalog.ALLOWED_TEST_TASKS` (task 072) |
| The command is built from the allowlist, never from model text | `AgentToolCatalog.commandFor` (task 072) |
| Command must stay inside the project | `ProjectBoundaryEnforcer`, `FORBIDDEN_COMMAND` |
| Write, install and git actions never retry automatically | `AgentRetryPolicy.neverAutoRetry` (task 079) |
| A command result that was not evaluated is not a pass | `TestOutcome.RAN_UNVERIFIED` (task 076) |
| Never assume access outside the project | `ProjectBoundary`, `ProjectAccessRegistry` |

---

## 4. What stays open

- **No measurement exists** for the Galaxy A56. Task 006 lists ARM64 / 8 GB RAM /
  128 GB UFS as a *presumption*. Every memory and timing number in this report is
  therefore either a project decision or absent — none is claimed as measured.
- **AVF availability on the A56** is unverified. "select devices" is all the source
  says, and it must not be read as "recent flagships".
- **Option D is undecided** and stays undecided. It needs the user, because it moves
  their source off the device.

## Sources

- Android Virtualization Framework overview —
  <https://source.android.com/docs/core/virtualization> (read 2026-10-02)
- AVF use cases, isolated compilation and Linux development environment —
  <https://source.android.com/docs/core/virtualization/usecases> (read 2026-10-02)
