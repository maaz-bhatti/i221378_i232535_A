# Custom Programming Language - Lexer Project

**Course:** CS4031 - Compiler Construction
**Assignment:** 01 - Lexical Analyzer Implementation

---

## 1. Team Members

* **Member 1:** Muhammad Harris Azhar - 23i-2535
* **Member 2:** Maaz Bin Usama - 22i-1578

---

## 2. Language Name and File Extension

* **Language Name:** FlexiCode
* **File Extension:** `.lang`

---

## 3. Keywords and Meanings

The language supports the following case-sensitive keywords:

| Keyword     | Meaning                                      |
| ----------- | -------------------------------------------- |
| `start`     | Marks the beginning of a program block.      |
| `finish`    | Marks the end of a program block.            |
| `declare`   | Used to define a new variable.               |
| `condition` | Used to start a conditional (if) statement.  |
| `else`      | Provides an alternative path for conditions. |
| `loop`      | Used to initiate iteration/repetition.       |
| `input`     | Reads data from the standard input.          |
| `output`    | Prints data to the standard output.          |
| `function`  | Defines a reusable block of code.            |
| `return`    | Returns a value from a function.             |
| `break`     | Exits the current loop.                      |
| `continue`  | Skips the current iteration of a loop.       |

---

## 4. Identifier Rules

Identifiers must follow these strict lexical rules:

* **Starting Character:** Must start with an uppercase letter (`A-Z`).
* **Subsequent Characters:** Can be lowercase letters, digits, or underscores.
* **Length:** Maximum of 31 characters total.

**Examples:**

* **Valid:** `Count`, `Variable_name`, `X`, `Total_sum_2024`
* **Invalid:** `count` (lowercase start), `2Count` (starts with digit), `myVariable` (lowercase start)

---

## 5. Literal Formats

**Integer Literals**

* **Regex:** `[+-]?[0-9]+`
* **Examples:** `42`, `+100`, `-567`, `0`

**Floating-Point Literals**

* **Regex:** `[+]?[0-9]+\.[0-9]{1,6}([eE][+-]?[0-9]+)?`
* **Examples:** `3.14`, `+2.5`, `1.5e10`, `2.0E-3`

**String and Character Literals**

* **String:** Enclosed in double quotes. Supports escape sequences: `\"`, `\\`, `\n`, `\t`, `\r`.
* **Character:** Enclosed in single quotes (e.g., `'a'`, `'\n'`).

**Boolean Literals**

* **Values:** `true`, `false` (case-sensitive)

---

## 6. Operators and Precedence

Listed in order of priority:

1. **Exponentiation:** `**`
2. **Arithmetic:** `*`, `/`, `%`, `+`, `-`
3. **Relational:** `==`, `!=`, `<=`, `>=`, `<`, `>`
4. **Logical:** `&&`, `||`, `!`
5. **Increment/Decrement:** `++`, `--`
6. **Assignment:** `=`, `+=`, `-=`, `*=`, `/=`

---

## 7. Comment Syntax

* **Single-line:** `## comment text`
* **Multi-line:** `#* comment *#`

---

## 8. Sample Programs

### Sample 1: Basic Arithmetic

```text
start
declare Result = (10 + 5) * 2
output Result
finish
```

### Sample 2: Loop and Condition

```text
start
declare X = 10
loop condition (X > 0)
    output X
    X--
finish
```

### Sample 3: String Handling

```text
start
declare Msg = "Processing..."
output Msg
finish
```

---

## 9. Compilation and Execution

### Requirements

* Java Development Kit (JDK)
* JFlex `.jar` file

### Steps

1. **Generate Scanner:** `java -jar jflex-1.x.x.jar Scanner.flex`
2. **Compile:** `javac Token.java Scanner.java Main.java SymbolTable.java`
3. **Run:** `java Main`
