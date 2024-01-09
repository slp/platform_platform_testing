# rdroidtest

This is a custom Rust test harness which allows tests to be ignored at runtime based on arbitrary
criteria. The built-in Rust test harness only allows tests to be ignored at compile time, but this
is often not enough on Android, where we want to ignore tests based on system properties or other
characteristics of the device on which the test is being run, which are not known at build time.

## Usage

Unfortunately without the built-in support that rustc provides to the standard test harness, this
one is slightly more cumbersome to use. Firstly, add it to the `rust_test` build rule in your
`Android.bp` by adding the defaults provided:

```soong
rust_test {
    name: "mycrate.test",
    defaults: ["rdroidtest.defaults"],
    // ...
}
```

If you are testing a binary that has a `main` function, you'll need to remove it from the test
build:

```rust
#[cfg(not(test))]
fn main() {
    // ...
}
```

(If you're testing a library or anything else which doesn't have a `main` function, you don't need
to worry about this.)

Each test case should be marked with the `rdroidtest::test!` macro, rather than the standard
`#[test]` attribute:

```rust
use rdroidtest::test;

test!(one_plus_one);
fn one_plus_one() {
    assert_eq!(1 + 1, 2);
}
```

To ignore a test, you can add an `ignore_if` clause with a boolean expression:

```rust
use rdroidtest::test;

test!(clap_hands, ignore_if: !feeling_happy());
fn clap_hands() {
    assert!(HANDS.clap().is_ok());
}
```

Somewhere in your main module, you need to use the `test_main` macro to generate an entry point for
the test harness:

```rust
rdroidtest::test_main!();
```

You can then run your tests as usual with `atest`.


## Parameterized Tests

To run the same test multiple times with different parameter values, use the `rdroidtest::ptest!`
macro:

```rust
use rdroidtest::ptest;

ptest!(is_even, my_instances());
fn is_even(param: u32) {
    assert_eq!(param % 2, 0);
}
```

The second argument to the `ptest!` macro is an expression that is called at runtime to generate
the set of parameters to invoke the test with.  This expression should emit a vector of
`(String, T)` values:

```rust
fn my_instances() -> Vec<(String, u32)> {
    vec![
        ("one".to_string(), 1),
        ("two".to_string(), 2),
        ("three".to_string(), 3),
    ]
}
```

The test method will be invoked with each of the parameter values in turn, passed in as the single
argument of type `T`.

Parameterized tests can also be ignored, using an `ignore_if` clause that accepts the parameter
value (this time as type `&T`) and returns a boolean:

```rust
ptest!(is_even_too, my_instances(), ignore_if: |p| feeling_odd(p));
fn is_even_too(param: u32) {
    assert_eq!(param % 2, 0);
}

fn feeling_odd(param: &u32) -> bool {
    *param % 2 == 1
}
```
