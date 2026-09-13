# 제목 추천

1. 멋사 백엔드 부트캠프 26기 Day 49 - 자바스크립트 ES6 핵심 문법 [1] — JSON·순회·스코프·클로저
2. 멋사 백엔드 부트캠프 26기 Day 49 - 자바스크립트 ES6 핵심 문법 [2] — 템플릿 리터럴·구조 분해·스프레드와 레스트

---

# 1편

## 📌 오늘의 목표

지난 시간에는 DOM을 탐색하고 이벤트를 처리하면서 자바스크립트로 화면을 제어하는 방법을 배웠다. 오늘은 화면을 조작하는 방법에서 한 걸음 더 나아가, **자바스크립트가 데이터를 표현하고 변수를 관리하는 방식**을 살펴보았다.

이번 1편의 목표는 다음과 같다.

- 자바스크립트 객체와 JSON 문자열의 차이를 구분한다.
- `JSON.stringify()`와 `JSON.parse()`의 변환 방향을 이해한다.
- Truthy와 Falsy를 이용한 조건 판단 방식을 이해한다.
- `for...in`과 `for...of`의 순회 대상을 구분한다.
- `var`, `let`, `const`의 스코프 차이를 이해한다.
- 외부 함수가 종료된 후에도 변수가 유지되는 클로저의 원리를 이해한다.

> 오늘 수업의 핵심은 새로운 문법을 외우는 것이 아니라, **값이 어떤 형태로 존재하고 어느 범위까지 살아 있는지 판단하는 것**이다.

---

## 1. 자바스크립트 데이터와 JSON

**자바스크립트의 기본형도 객체처럼 사용할 수 있는 이유**

자바스크립트에는 문자열, 숫자, 불리언과 같은 기본형 값이 있다.

```javascript
const name = "americano";
console.log(name.length); // 9
```

`name`은 문자열 기본형인데도 객체의 속성처럼 `.length`를 사용할 수 있다. 자바스크립트가 필요한 순간에 기본형 값을 임시 래퍼 객체처럼 다룰 수 있도록 변환하기 때문이다. 이러한 동작을 **오토 박싱(Autoboxing)**이라고 한다.

```text
문자열 기본형 값
      ↓ 필요한 순간 임시로 감싸기
String 객체처럼 사용
      ↓ 속성 또는 메서드 사용
다시 기본형 값으로 처리
```

예를 들어 문자열에서 다음과 같은 속성과 메서드를 사용할 수 있다.

```javascript
const text = "hello";

console.log(text.length);        // 5
console.log(text.toUpperCase()); // HELLO
```

다만 기본형 값 자체가 계속 객체로 저장된다는 뜻은 아니다. 자바스크립트 엔진이 필요한 순간에 객체처럼 사용할 수 있도록 처리해 주는 것이다.

**자바스크립트 객체와 JSON은 같은 것일까?**

겉모습이 비슷해서 가장 많이 혼동하는 부분이다.

```javascript
const menu = {
  name: "아메리카노",
  price: 2000
};
```

위 코드는 자바스크립트에서 실제 객체를 만드는 **객체 리터럴**이다. 반면 JSON은 데이터를 저장하거나 전달하기 위한 **문자열 기반의 데이터 표기 형식**이다.

```json
{
  "name": "아메리카노",
  "price": 2000
}
```

둘은 모양은 비슷하지만 역할과 규칙이 다르다.

| 구분 | 자바스크립트 객체 | JSON |
| --- | --- | --- |
| 정체 | 프로그램에서 사용하는 객체 | 데이터를 표현한 문자열 형식 |
| 속성 이름 | 따옴표 생략 가능 | 반드시 큰따옴표 사용 |
| 함수 저장 | 가능 | 불가능 |
| 주석 | 코드에서는 작성 가능 | JSON 문법에서는 불가능 |
| 주요 용도 | 프로그램 내부 로직 | API 통신, 파일 저장, 데이터 교환 |

따라서 “자바스크립트 객체가 곧 JSON이다”라고 이해하면 안 된다. **객체를 JSON 문자열로 바꾸거나 JSON 문자열을 객체로 복원할 수 있는 관계**라고 이해하는 것이 정확하다.

**직렬화와 역직렬화**

브라우저에서 만든 객체를 서버로 전송하려면 객체를 그대로 보내기보다 문자열 형태로 변환해야 한다. 객체를 저장하거나 전송하기 좋은 문자열로 바꾸는 과정을 **직렬화(Serialization)**라고 한다.

```javascript
const menu = {
  name: "아메리카노",
  price: 2000
};

const jsonText = JSON.stringify(menu);

console.log(jsonText);
// {"name":"아메리카노","price":2000}
```

반대로 서버에서 받은 JSON 문자열을 자바스크립트 객체로 복원하는 과정을 **역직렬화(Deserialization)**라고 한다.

```javascript
const jsonText = '{"name":"아메리카노","price":2000}';
const menuObject = JSON.parse(jsonText);

console.log(menuObject.name);  // 아메리카노
console.log(menuObject.price); // 2000
```

변환 방향은 다음과 같이 기억하면 된다.

```text
자바스크립트 객체
      │
      │ JSON.stringify()
      ▼
   JSON 문자열
      │
      │ JSON.parse()
      ▼
자바스크립트 객체
```

| 메서드 | 변환 방향 | 의미 |
| --- | --- | --- |
| `JSON.stringify()` | 객체 → JSON 문자열 | 직렬화 |
| `JSON.parse()` | JSON 문자열 → 객체 | 역직렬화 |

**왜 `eval()` 대신 `JSON.parse()`를 사용할까?**

`eval()`은 문자열을 자바스크립트 코드로 해석하여 실행한다. 따라서 전달받은 문자열 안에 악의적인 코드가 포함되어 있다면 그 코드까지 실행될 수 있다.

```javascript
// 외부에서 받은 문자열을 이렇게 실행하면 위험하다.
eval(receivedText);
```

JSON 데이터를 처리하려는 목적이라면 JSON 문법만 분석하는 `JSON.parse()`를 사용해야 한다.

```javascript
const data = JSON.parse(receivedText);
```

`JSON.parse()`도 잘못된 JSON 문자열을 받으면 `SyntaxError`를 발생시킨다. 실제 서비스에서는 필요에 따라 `try...catch`로 오류를 처리해야 한다.

```javascript
try {
  const data = JSON.parse(receivedText);
  console.log(data);
} catch (error) {
  console.log("올바르지 않은 JSON 형식입니다.");
}
```

> **이미지 1 삽입 위치**  
> 이 문단 아래에 `객체 → JSON.stringify() → JSON 문자열 → JSON.parse() → 객체` 변환 흐름 이미지를 넣으면 직렬화와 역직렬화의 방향을 한눈에 이해하기 좋다.

---

## 2. Truthy와 Falsy

**조건식에는 불리언만 사용할 수 있을까?**

자바의 `if` 조건식에는 `true` 또는 `false`가 되는 불리언 표현식이 필요하다. 자바스크립트는 다른 자료형의 값도 조건식에 사용할 수 있으며, 값을 불리언처럼 판단한다.

```javascript
const name = "아메리카노";

if (name) {
  console.log("메뉴 이름이 있습니다.");
}
```

`name`은 불리언이 아니라 문자열이지만, 비어 있지 않은 문자열이므로 참처럼 평가된다. 이처럼 조건식에서 참으로 평가되는 값을 **Truthy**, 거짓으로 평가되는 값을 **Falsy**라고 한다.

대표적인 Falsy 값은 다음과 같다.

```javascript
false
0
-0
0n
""
null
undefined
NaN
```

위 값을 제외한 대부분의 값은 Truthy로 평가된다. 특히 비어 있는 배열과 객체도 Truthy라는 점을 주의해야 한다.

```javascript
if ([]) {
  console.log("빈 배열도 Truthy입니다.");
}

if ({}) {
  console.log("빈 객체도 Truthy입니다.");
}
```

**논리 연산자는 항상 true 또는 false를 반환할까?**

자바스크립트의 `&&`와 `||`는 반드시 불리언만 반환하지 않는다. 평가를 멈춘 시점의 **실제 피연산자 값**을 반환한다.

```javascript
console.log("hello" || "default"); // hello
console.log("" || "default");      // default

console.log("hello" && "world");  // world
console.log("" && "world");       // ""
```

`||`는 왼쪽 값이 Truthy이면 왼쪽 값을 반환하고, Falsy이면 오른쪽 값을 반환한다.

```javascript
const inputName = "";
const name = inputName || "이름 없음";

console.log(name); // 이름 없음
```

간단한 기본값을 설정할 때 유용하지만, `0`, 빈 문자열, `false`도 모두 Falsy로 판단한다. 이 값들이 정상 데이터가 될 수 있다면 2편에서 살펴볼 널 병합 연산자 `??`가 더 적절하다.

---

## 3. 데이터를 순회하는 방법

**일반 for문**

배열을 순회하는 가장 기본적인 방법은 인덱스를 사용하는 일반 `for`문이다.

```javascript
const scores = [70, 80, 90];

for (let i = 0; i < scores.length; i += 1) {
  console.log(i, scores[i]);
}
```

인덱스가 필요한 경우에는 유용하지만, 값만 필요하다면 `scores[i]`를 반복해서 작성해야 한다.

**`for...in`: 키를 순회**

`for...in`은 객체의 열거 가능한 **키(Property Key)**를 순회한다.

```javascript
const menu = {
  name: "아메리카노",
  price: 2000
};

for (const key in menu) {
  console.log(key, menu[key]);
}
```

실행 결과는 다음과 같다.

```text
name 아메리카노
price 2000
```

`key`에는 값이 아니라 `name`, `price`와 같은 속성 이름이 들어간다. 따라서 값에 접근할 때는 `menu[key]`처럼 대괄호 표기법을 사용한다.

배열에도 `for...in`을 사용할 수 있지만 반환되는 것은 요소가 아니라 문자열 형태의 인덱스 키다.

```javascript
const scores = [70, 80, 90];

for (const index in scores) {
  console.log(typeof index, index);
}

// string 0
// string 1
// string 2
```

배열의 실제 값을 순회하려는 목적이라면 `for...of`가 더 자연스럽다.

**`for...of`: 값을 순회**

`for...of`는 반복 가능한 객체의 **값**을 순회한다.

```javascript
const scores = [70, 80, 90];

for (const score of scores) {
  console.log(score);
}
```

배열뿐 아니라 문자열, `Map`, `Set`처럼 반복 가능한 객체에 사용할 수 있다.

```javascript
const text = "ABC";

for (const character of text) {
  console.log(character);
}
```

일반 객체는 기본적으로 반복 가능한 객체가 아니므로 바로 `for...of`를 사용할 수 없다.

```javascript
const menu = {
  name: "아메리카노",
  price: 2000
};

// TypeError 발생
// for (const value of menu) {
//   console.log(value);
// }
```

일반 객체의 값을 `for...of`로 순회하려면 `Object.keys()`, `Object.values()`, `Object.entries()` 등으로 배열 형태를 만든다.

```javascript
for (const [key, value] of Object.entries(menu)) {
  console.log(key, value);
}
```

**`Map`과 `Set`**

`Map`은 키와 값을 한 쌍으로 저장하는 컬렉션이다.

```javascript
const menuMap = new Map();

menuMap.set("americano", 2000);
menuMap.set("latte", 3500);

for (const [name, price] of menuMap) {
  console.log(`${name}: ${price}원`);
}
```

`Map`을 `for...of`로 순회하면 `[키, 값]` 형태의 엔트리를 받는다. 배열 구조 분해를 함께 사용하면 키와 값을 의미 있는 변수명으로 바로 꺼낼 수 있다.

`Set`은 중복되지 않는 값을 관리한다.

```javascript
const categories = new Set(["커피", "차", "커피"]);

for (const category of categories) {
  console.log(category);
}

// 커피
// 차
```

순회 문법은 다음 기준으로 선택할 수 있다.

| 필요한 것 | 적합한 방법 |
| --- | --- |
| 배열의 인덱스와 값을 모두 직접 제어 | 일반 `for`문 |
| 일반 객체의 속성 키 | `for...in` |
| 배열·문자열·Map·Set의 값 | `for...of` |
| 일반 객체의 키와 값 | `Object.entries()` + `for...of` |

---

## 4. 변수 선언과 스코프

**선언하지 않은 변수의 문제**

다음 코드는 함수 안에서 `value`에 값을 대입하고 있지만 변수를 선언하지 않았다.

```javascript
function run() {
  value = 10;
}

run();
console.log(value);
```

느슨한 실행 환경에서는 선언하지 않은 식별자에 값을 대입할 경우 전역 객체의 속성이 만들어질 수 있다. 브라우저의 대표적인 전역 객체는 `window`이며, 여러 실행 환경에서 공통으로 접근할 때는 `globalThis`를 사용할 수 있다.

전역 변수는 여러 함수가 함께 접근하고 수정할 수 있기 때문에 다음과 같은 문제가 생긴다.

- 어디에서 값이 변경되었는지 추적하기 어렵다.
- 같은 이름을 사용한 다른 코드와 충돌할 수 있다.
- 함수가 외부 상태에 의존하여 재사용하기 어려워진다.

엄격 모드에서는 선언하지 않은 변수에 값을 대입하면 오류가 발생한다.

```javascript
"use strict";

function run() {
  value = 10; // ReferenceError
}
```

따라서 변수는 반드시 `let` 또는 `const`로 명시적으로 선언해야 한다.

**`var`의 함수 스코프**

`var`는 함수 단위의 스코프를 가진다. `if`문이나 `for`문의 중괄호는 독립된 스코프를 만들지 않는다.

```javascript
if (true) {
  var count = 10;
}

console.log(count); // 10
```

또한 같은 스코프 안에서 중복 선언할 수 있다.

```javascript
var count = 10;
var count = 20;

console.log(count); // 20
```

코드가 길어질수록 같은 이름의 변수가 다시 선언되어도 오류가 발생하지 않아 실수를 찾기 어려워질 수 있다.

**`let`의 블록 스코프**

`let`은 중괄호 `{}`를 기준으로 하는 블록 스코프를 가진다.

```javascript
if (true) {
  let count = 10;
  console.log(count); // 10
}

// console.log(count); // ReferenceError
```

같은 블록 안에서 같은 이름으로 중복 선언할 수도 없다.

```javascript
let count = 10;
// let count = 20; // SyntaxError
```

**`const`는 값을 절대 바꾸지 못한다는 뜻일까?**

`const`는 선언과 동시에 초기화해야 하며, 변수에 다른 값을 다시 대입할 수 없다.

```javascript
const count = 10;
// count = 20; // TypeError
```

그러나 객체나 배열의 내부 내용까지 자동으로 불변으로 만드는 것은 아니다.

```javascript
const menu = {
  name: "아메리카노",
  price: 2000
};

menu.price = 2500; // 가능

// menu = {}; // 불가능
```

`const`가 막는 것은 객체 내부 변경이 아니라 **변수가 다른 객체를 가리키도록 참조를 재할당하는 것**이다.

```text
const menu ──────▶ { name: "아메리카노", price: 2000 }
   │                              │
   │ 다른 객체로 변경 불가          │ 내부 속성 변경 가능
   └────────────── X              └────────────── O
```

기본 원칙은 다음과 같이 정리할 수 있다.

1. 우선 `const`로 선언한다.
2. 이후 변수 자체에 다른 값을 다시 대입해야 한다면 `let`을 사용한다.
3. 특별한 이유가 없다면 새 코드에서 `var` 사용은 피한다.

| 선언 키워드 | 스코프 | 재선언 | 재할당 |
| --- | --- | --- | --- |
| `var` | 함수 스코프 | 가능 | 가능 |
| `let` | 블록 스코프 | 불가능 | 가능 |
| `const` | 블록 스코프 | 불가능 | 불가능 |

---

## 5. 아우터 변수와 클로저

**내부 함수는 어디의 변수까지 사용할 수 있을까?**

함수 안에 선언된 함수를 **내부 함수**, 내부 함수를 감싸는 함수를 **외부 함수**라고 한다.

```javascript
const globalValue = 10;

function outer() {
  const outerValue = 20;

  function inner() {
    const innerValue = 30;

    console.log(innerValue);  // 자신의 지역 변수
    console.log(outerValue);  // 외부 함수의 지역 변수
    console.log(globalValue); // 전역 변수
  }

  inner();
}

outer();
```

내부 함수는 자신의 지역 범위에서 변수를 먼저 찾고, 없다면 자신을 감싸는 외부 범위로 올라가며 찾는다. 이러한 연결 관계를 **스코프 체인(Scope Chain)**이라고 한다.

**외부 함수가 끝나면 지역 변수도 사라지지 않을까?**

일반적으로 함수의 지역 변수는 함수가 호출될 때 만들어지고 실행이 끝나면 더 이상 사용할 수 없다.

```javascript
function outer() {
  const value = 10;
}

outer();
// console.log(value); // 접근 불가
```

그러나 내부 함수가 외부 함수의 지역 변수를 참조한 상태로 외부에 반환되면 상황이 달라진다.

```javascript
function createCounter() {
  let count = 0;

  return function () {
    count += 1;
    return count;
  };
}

const counter = createCounter();

console.log(counter()); // 1
console.log(counter()); // 2
console.log(counter()); // 3
```

실행 흐름을 단계별로 살펴보면 다음과 같다.

1. `createCounter()`가 호출된다.
2. 지역 변수 `count`가 만들어지고 `0`이 저장된다.
3. `count`를 사용하는 내부 함수가 반환된다.
4. `createCounter()`의 실행은 종료된다.
5. 반환된 내부 함수가 여전히 `count`를 참조하므로 `count`는 유지된다.
6. `counter()`를 호출할 때마다 같은 `count`가 1씩 증가한다.

이처럼 **함수가 선언될 당시의 외부 렉시컬 환경을 기억하여, 외부 함수 실행이 끝난 뒤에도 그 변수에 접근할 수 있는 현상**을 클로저라고 한다.

```text
createCounter() 실행 종료
          │
          ▼
counter 함수 ─────────▶ count 변수
                           │
                           ├─ 첫 호출: 1
                           ├─ 둘째 호출: 2
                           └─ 셋째 호출: 3
```

**클로저는 왜 사용할까?**

전역 변수를 사용하면 어디에서나 값을 변경할 수 있다.

```javascript
let count = 0;

function increase() {
  count += 1;
}
```

반면 클로저를 사용하면 상태를 함수 내부에 숨기고, 허용된 함수를 통해서만 변경할 수 있다.

```javascript
function createCounter() {
  let count = 0;

  return {
    increase() {
      count += 1;
      return count;
    },
    current() {
      return count;
    }
  };
}

const counter = createCounter();

console.log(counter.increase()); // 1
console.log(counter.current());  // 1
```

외부에서는 `count`에 직접 접근할 수 없고 `increase()`와 `current()`를 통해서만 사용할 수 있다. 따라서 클로저는 다음과 같은 경우에 활용된다.

- 여러 함수 호출 사이에서 상태를 유지할 때
- 전역 변수 사용을 줄일 때
- 외부에서 직접 변경하면 안 되는 값을 숨길 때
- 이벤트 핸들러나 비동기 콜백에서 특정 값을 기억할 때

**반복문에서 `var`와 클로저가 만나면 생기는 문제**

다음 코드는 세 개의 함수를 배열에 저장한다.

```javascript
const functions = [];

for (var i = 0; i < 3; i += 1) {
  functions.push(function () {
    console.log(i);
  });
}

functions[0](); // 3
functions[1](); // 3
functions[2](); // 3
```

`var`는 블록 스코프가 없으므로 반복할 때마다 새로운 `i`가 만들어지지 않는다. 세 함수가 모두 같은 `i`를 참조하고, 반복문이 끝났을 때 `i`는 `3`이 되어 있다.

`let`을 사용하면 반복마다 독립적인 블록 스코프가 만들어진다.

```javascript
const functions = [];

for (let i = 0; i < 3; i += 1) {
  functions.push(function () {
    console.log(i);
  });
}

functions[0](); // 0
functions[1](); // 1
functions[2](); // 2
```

따라서 이벤트 처리나 비동기 콜백처럼 함수가 나중에 실행되는 코드에서는 변수가 어느 스코프에 만들어졌고 어떤 값을 참조하는지 확인해야 한다.

> **이미지 2 삽입 위치**  
> 이 문단 아래에 `counter 함수가 외부 함수의 count 변수를 계속 참조하는 그림`을 넣으면 클로저를 단순히 “함수 안의 함수”로 오해하는 일을 줄일 수 있다.

---

## 📎 출처

본 글은 **뉴렉처(Newlecture) 박용우 강사님의 강의 자료 및 수업 내용**을 바탕으로 학습한 내용을 정리한 글입니다.

- 뉴렉처: [https://www.newlecture.com](https://www.newlecture.com)

---

# 2편

## 📌 오늘의 목표

1편에서는 자바스크립트의 데이터 표현 방식부터 JSON 변환, Truthy와 Falsy, 순회 문법, 변수의 스코프와 클로저까지 살펴보았다.

2편에서는 ES6 이후 추가된 문법을 이용해 기존 코드를 더 간결하고 의도가 잘 드러나는 형태로 바꾸는 방법을 정리한다.

- 템플릿 리터럴로 문자열을 읽기 쉽게 작성한다.
- `||`와 `??`의 기본값 처리 기준을 구분한다.
- 옵셔널 체이닝으로 선택적 데이터에 안전하게 접근한다.
- 객체와 배열에서 필요한 값을 구조 분해로 꺼낸다.
- 스프레드와 레스트의 반대 방향을 이해한다.
- 단순히 짧은 코드가 아니라 의미가 분명한 코드를 작성한다.

---

## 1. 템플릿 리터럴

**기존 문자열 연결의 문제**

문자열 안에 변수 값을 넣기 위해 `+` 연산자를 여러 번 사용하면 문자열과 변수의 경계가 복잡해진다.

```javascript
const name = "아메리카노";
const price = 2000;

const message = "메뉴는 " + name + "이고 가격은 " + price + "원입니다.";
```

값이 많아질수록 따옴표, 공백, `+` 연산자의 위치를 확인하기 어려워진다.

**백틱과 문자열 보간**

템플릿 리터럴은 작은따옴표나 큰따옴표 대신 백틱을 사용한다. `${표현식}` 안에 변수나 계산식을 넣을 수 있다.

```javascript
const name = "아메리카노";
const price = 2000;

const message = `메뉴는 ${name}이고 가격은 ${price}원입니다.`;

console.log(message);
```

`${}` 안에는 변수뿐 아니라 계산식과 함수 호출도 사용할 수 있다.

```javascript
const price = 2000;
const count = 3;

console.log(`총금액은 ${price * count}원입니다.`);
```

템플릿 리터럴은 여러 줄 문자열도 그대로 작성할 수 있다.

```javascript
const html = `
  <article class="menu-card">
    <h1>아메리카노</h1>
    <p>2,000원</p>
  </article>
`;
```

DOM에 넣을 HTML 조각이나 여러 줄의 안내 문구를 만들 때 유용하다. 다만 사용자 입력을 그대로 HTML 문자열에 삽입하면 XSS 문제가 생길 수 있으므로, 신뢰할 수 없는 값은 별도의 안전한 처리 없이 `innerHTML`에 넣지 않아야 한다.

**`String.raw`**

문자열 안의 `\n`, `\t`와 같은 역슬래시 문자는 일반적으로 이스케이프 시퀀스로 해석된다.

```javascript
console.log("hello\nworld");
```

실행 결과:

```text
hello
world
```

`String.raw`를 템플릿 리터럴 앞에 붙이면 역슬래시를 원시 문자열처럼 다룰 수 있다.

```javascript
const path = String.raw`C:\temp\new-folder`;

console.log(path);
// C:\temp\new-folder
```

운영체제 경로나 정규표현식 예시처럼 역슬래시 자체를 표시해야 할 때 유용하다.

---

## 2. 기본값과 안전한 접근

**논리 OR 연산자를 이용한 기본값**

`||`는 왼쪽 값이 Falsy이면 오른쪽 값을 반환한다.

```javascript
const inputName = "";
const name = inputName || "이름 없음";

console.log(name); // 이름 없음
```

문제는 `0`, 빈 문자열, `false`가 실제로 유효한 데이터여도 기본값으로 교체된다는 것이다.

```javascript
const count = 0;
const result = count || 10;

console.log(result); // 10
```

상품 수량이 `0`인 상황을 정상 값으로 유지해야 한다면 위 코드는 의도와 다르다.

**널 병합 연산자 `??`**

널 병합 연산자는 왼쪽 값이 `null` 또는 `undefined`일 때만 오른쪽 기본값을 사용한다.

```javascript
const count = 0;
const result = count ?? 10;

console.log(result); // 0
```

```javascript
const discountRate = null;
const result = discountRate ?? 0;

console.log(result); // 0
```

둘의 판단 기준은 다음과 같다.

| 값 | `value || "기본값"` | `value ?? "기본값"` |
| --- | --- | --- |
| `0` | 기본값 | `0` 유지 |
| `""` | 기본값 | 빈 문자열 유지 |
| `false` | 기본값 | `false` 유지 |
| `null` | 기본값 | 기본값 |
| `undefined` | 기본값 | 기본값 |

따라서 선택 기준은 간단하다.

- Falsy 값 전체를 “값이 없음”으로 처리한다면 `||`
- `null`과 `undefined`만 “값이 없음”으로 처리한다면 `??`

**옵셔널 체이닝 `?.`**

중첩된 객체의 속성에 접근할 때 중간 값이 없다면 오류가 발생한다.

```javascript
const menu = {};

// TypeError
// console.log(menu.category.name);
```

기존에는 `&&`를 이용하여 각 값이 존재하는지 확인했다.

```javascript
const categoryName = menu && menu.category && menu.category.name;
```

옵셔널 체이닝을 사용하면 더 간단하게 작성할 수 있다.

```javascript
const categoryName = menu?.category?.name;

console.log(categoryName); // undefined
```

중간 값이 `null` 또는 `undefined`라면 오류를 발생시키는 대신 `undefined`를 반환한다. `??`와 조합하면 기본값까지 지정할 수 있다.

```javascript
const categoryName = menu?.category?.name ?? "분류 없음";
```

선택적으로 존재하는 함수도 안전하게 호출할 수 있다.

```javascript
const options = {};

options.onComplete?.();
```

다만 옵셔널 체이닝은 모든 오류를 감추기 위한 문법이 아니다. 반드시 있어야 하는 데이터에 `?.`를 사용하면 데이터 오류를 늦게 발견할 수 있다.

- 값이 없어도 정상인 선택적 데이터: `?.` 사용 가능
- 반드시 존재해야 하는 필수 데이터: 일반 접근으로 문제를 빠르게 확인

---

## 3. 객체 구조 분해

**반복되는 객체 접근 줄이기**

다음 코드는 같은 객체 이름을 반복해서 사용한다.

```javascript
const exam = {
  korean: 20,
  english: 20,
  math: 30
};

const total = exam.korean + exam.english + exam.math;
```

객체 구조 분해를 사용하면 속성의 값을 같은 이름의 변수로 꺼낼 수 있다.

```javascript
const { korean, english, math } = exam;
const total = korean + english + math;
```

객체 구조 분해는 **속성 이름을 기준으로** 값을 찾는다. 작성 순서는 중요하지 않다.

```javascript
const { math, korean, english } = exam;
```

**다른 변수 이름으로 꺼내기**

속성명과 다른 변수명을 사용하려면 콜론 뒤에 새 이름을 작성한다.

```javascript
const menu = {
  name: "아메리카노",
  price: 2000
};

const { name: menuName, price: menuPrice } = menu;

console.log(menuName);  // 아메리카노
console.log(menuPrice); // 2000
```

이 문법에서 `name`과 `price`라는 변수가 만들어지는 것이 아니라 `menuName`과 `menuPrice`가 만들어진다.

**기본값 지정하기**

해당 속성이 `undefined`라면 기본값을 사용할 수 있다.

```javascript
const menu = {
  name: "아메리카노"
};

const { name, price = 0 } = menu;

console.log(price); // 0
```

기본값은 속성값이 `undefined`일 때 적용된다. 속성값이 `null`이라면 `null`이 그대로 들어간다.

**함수 매개변수에서 구조 분해하기**

함수가 객체 전체를 받지만 일부 속성만 사용한다면 매개변수 위치에서 바로 구조 분해할 수 있다.

```javascript
function printExam({ korean, english, math }) {
  const total = korean + english + math;
  console.log(`총점은 ${total}점입니다.`);
}

printExam({
  korean: 20,
  english: 20,
  math: 30
});
```

함수가 어떤 속성을 필요로 하는지 선언부에서 바로 확인할 수 있다는 장점이 있다.

**축약 속성과 메서드 축약**

객체를 만들 때 변수명과 속성명이 같다면 한 번만 작성할 수 있다.

```javascript
const korean = 20;
const english = 20;
const math = 30;

// 기존 방식
const oldExam = {
  korean: korean,
  english: english,
  math: math
};

// 축약 속성
const exam = {
  korean,
  english,
  math
};
```

객체 메서드도 `function` 키워드를 생략하여 작성할 수 있다.

```javascript
const exam = {
  korean,
  english,
  math,
  total() {
    return this.korean + this.english + this.math;
  }
};
```

`this`는 언제나 같은 객체를 가리키는 것이 아니라 **함수가 호출된 방식**에 따라 달라진다. 위처럼 `exam.total()`로 호출하면 메서드 안의 `this`는 호출 주체인 `exam`을 가리킨다.

```javascript
console.log(exam.total());
```

객체 메서드에서 `this`를 사용해야 한다면 화살표 함수와의 차이도 주의해야 한다. 화살표 함수는 자신만의 `this`를 만들지 않으므로 객체 메서드를 무조건 화살표 함수로 바꾸면 의도와 다른 결과가 나올 수 있다.

---

## 4. 배열 구조 분해

**위치를 기준으로 값 꺼내기**

배열 구조 분해는 객체와 달리 **요소의 위치**를 기준으로 값을 꺼낸다.

```javascript
const scores = [10, 30, 40];
const [korean, english, math] = scores;

console.log(korean);  // 10
console.log(english); // 30
console.log(math);    // 40
```

객체 구조 분해와 배열 구조 분해의 기준을 구분해야 한다.

| 구분 | 값을 찾는 기준 |
| --- | --- |
| 객체 구조 분해 | 속성 이름 |
| 배열 구조 분해 | 요소 위치 |

**일부 요소 건너뛰기**

쉼표를 이용하면 필요하지 않은 위치를 건너뛸 수 있다.

```javascript
const scores = [10, 30, 40];
const [korean, , math] = scores;

console.log(korean); // 10
console.log(math);   // 40
```

하지만 쉼표가 많아지면 어떤 값을 생략했는지 읽기 어려워질 수 있으므로 과도하게 사용하지 않는 것이 좋다.

**두 변수의 값 교환하기**

기존에는 임시 변수를 사용하여 두 값을 교환했다.

```javascript
let first = 10;
let second = 20;

const temp = first;
first = second;
second = temp;
```

배열 구조 분해를 사용하면 한 줄로 교환할 수 있다.

```javascript
let first = 10;
let second = 20;

[first, second] = [second, first];

console.log(first);  // 20
console.log(second); // 10
```

오른쪽에서 `[second, first]`라는 새 배열을 만들고, 왼쪽의 구조 분해를 통해 각 위치의 값을 다시 변수에 할당하는 원리다.

---

## 5. 스프레드와 레스트

**같은 `...`인데 역할이 다른 이유**

스프레드와 레스트는 모두 `...` 기호를 사용한다. 하지만 어느 위치에 사용했는지에 따라 방향이 반대다.

```text
스프레드: 묶여 있는 값을 펼친다.
레스트:   남아 있는 값을 하나로 모은다.
```

**스프레드: 펼치기**

함수가 세 개의 독립된 인자를 받는다고 가정해 보자.

```javascript
function printNumbers(first, second, third) {
  console.log(first, second, third);
}
```

배열 자체를 전달하면 첫 번째 매개변수에 배열 전체가 들어간다.

```javascript
const numbers = [3, 5, 6];

printNumbers(numbers);
// [3, 5, 6] undefined undefined
```

스프레드 문법을 사용하면 배열의 각 요소를 독립된 인자로 펼칠 수 있다.

```javascript
printNumbers(...numbers);
// 3 5 6
```

배열을 복사하거나 결합할 때도 사용할 수 있다.

```javascript
const first = [1, 2];
const second = [3, 4];

const copied = [...first];
const combined = [...first, ...second];

console.log(copied);   // [1, 2]
console.log(combined); // [1, 2, 3, 4]
```

객체의 속성도 새 객체에 펼칠 수 있다.

```javascript
const menu = {
  name: "아메리카노",
  price: 2000
};

const changedMenu = {
  ...menu,
  price: 2500
};

console.log(changedMenu);
// { name: "아메리카노", price: 2500 }
```

같은 속성이 여러 번 등장하면 뒤에 작성한 값이 앞의 값을 덮어쓴다. 따라서 `...menu`와 `price: 2500`의 순서가 중요하다.

스프레드 복사는 한 단계만 복사하는 **얕은 복사**다. 중첩 객체까지 모두 새 객체로 복제하는 것은 아니다.

```javascript
const menu = {
  name: "아메리카노",
  option: {
    ice: true
  }
};

const copiedMenu = { ...menu };
copiedMenu.option.ice = false;

console.log(menu.option.ice); // false
```

`menu.option`과 `copiedMenu.option`이 같은 중첩 객체를 참조하기 때문이다.

**레스트: 나머지 모으기**

함수 선언부의 레스트 매개변수는 앞의 고정 매개변수에 들어가지 않은 나머지 인자를 배열로 모은다.

```javascript
function printNumbers(first, second, ...rest) {
  console.log(first);  // 3
  console.log(second); // 5
  console.log(rest);   // [6, 7, 8]
}

printNumbers(3, 5, 6, 7, 8);
```

레스트 매개변수는 반드시 마지막에 한 번만 작성해야 한다.

```javascript
// 잘못된 문법
// function printNumbers(...rest, last) {}
```

**`arguments`와 레스트 매개변수**

일반 함수 내부에서는 `arguments`로 전달된 인자들을 확인할 수 있다.

```javascript
function printAll() {
  console.log(arguments);
}

printAll(1, 2, 3);
```

하지만 `arguments`는 실제 배열이 아니므로 배열 메서드를 바로 사용하는 데 제약이 있다. 또한 화살표 함수는 자신만의 `arguments`를 가지지 않는다.

레스트 매개변수는 실제 배열이므로 `map()`, `filter()`, `reduce()` 같은 배열 메서드를 바로 사용할 수 있다.

```javascript
function sum(...numbers) {
  return numbers.reduce((total, number) => total + number, 0);
}

console.log(sum(10, 20, 30)); // 60
```

**구조 분해와 레스트 조합**

배열 구조 분해에서 앞의 값만 따로 꺼내고 나머지를 배열로 모을 수 있다.

```javascript
const scores = [90, 80, 70, 60];
const [first, second, ...rest] = scores;

console.log(first);  // 90
console.log(second); // 80
console.log(rest);   // [70, 60]
```

객체 구조 분해에서도 일부 속성을 꺼내고 나머지 속성을 새 객체로 모을 수 있다.

```javascript
const menu = {
  id: 1,
  name: "아메리카노",
  price: 2000
};

const { id, ...menuInfo } = menu;

console.log(id);       // 1
console.log(menuInfo); // { name: "아메리카노", price: 2000 }
```

전체 사용 위치를 표로 정리하면 다음과 같다.

| 사용 위치 | 이름 | 역할 | 예시 |
| --- | --- | --- | --- |
| 함수 호출부 | 스프레드 | 배열 요소를 개별 인자로 펼침 | `fn(...items)` |
| 배열 리터럴 | 스프레드 | 기존 배열 요소를 새 배열에 펼침 | `[...items]` |
| 객체 리터럴 | 스프레드 | 기존 객체 속성을 새 객체에 펼침 | `{ ...menu }` |
| 함수 선언부 | 레스트 | 남은 인자를 배열로 모음 | `function fn(...items)` |
| 구조 분해 왼쪽 | 레스트 | 남은 요소나 속성을 모음 | `const [first, ...rest] = items` |

> **이미지 3 삽입 위치**  
> 이 표 아래에 `배열 [1, 2, 3]이 스프레드로 펼쳐지는 방향`과 `여러 인자가 레스트 배열로 모이는 방향`을 좌우로 비교한 이미지를 넣으면 두 문법을 기억하기 쉽다.

---

## 6. Before → After

오늘 배운 문법은 단순히 코드를 짧게 만드는 장식이 아니다. 데이터에서 필요한 의미를 드러내고, 값이 없을 때의 처리 기준을 명확하게 만드는 도구다.

**Before: 반복 접근과 문자열 연결**

```javascript
function printMenu(menu) {
  const name = menu && menu.name ? menu.name : "이름 없음";
  const price = menu && menu.price ? menu.price : 0;

  console.log("메뉴는 " + name + "이고 가격은 " + price + "원입니다.");
}
```

이 코드는 다음과 같은 문제가 있다.

- `menu`를 반복해서 접근한다.
- 삼항 연산자와 문자열 연결이 섞여 읽기 어렵다.
- 가격이 정상 값 `0`이어도 Falsy로 판단한다.

**After: 구조 분해와 ES6 문법 활용**

```javascript
function printMenu(menu = {}) {
  const {
    name = "이름 없음",
    price = 0
  } = menu;

  console.log(`메뉴는 ${name}이고 가격은 ${price}원입니다.`);
}
```

중첩된 선택적 데이터까지 있다면 다음처럼 작성할 수 있다.

```javascript
function printMenu(menu = {}) {
  const { name = "이름 없음", price = 0 } = menu;
  const categoryName = menu.category?.name ?? "분류 없음";

  console.log(
    `메뉴는 ${name}, 가격은 ${price}원, 분류는 ${categoryName}입니다.`
  );
}
```

코드가 짧아진 것보다 중요한 변화는 다음과 같다.

- 함수가 어떤 데이터를 사용하는지 명확하다.
- `0`과 값의 부재를 구분한다.
- 선택적 속성에 접근하는 의도가 드러난다.
- 출력 문장의 구조를 한눈에 읽을 수 있다.

---

## ✅ 핵심 정리

1. 자바스크립트 객체와 JSON은 모양이 비슷하지만 같은 것이 아니다.
2. `JSON.stringify()`는 객체를 JSON 문자열로 직렬화한다.
3. `JSON.parse()`는 JSON 문자열을 자바스크립트 객체로 역직렬화한다.
4. 조건식에서 참처럼 평가되는 값은 Truthy, 거짓처럼 평가되는 값은 Falsy다.
5. `for...in`은 주로 객체의 키를, `for...of`는 반복 가능한 객체의 값을 순회한다.
6. 새 코드에서는 기본적으로 `const`를 사용하고, 재할당이 필요할 때 `let`을 사용한다.
7. 클로저는 내부 함수가 외부 렉시컬 환경의 변수를 기억하는 현상이다.
8. 템플릿 리터럴은 `${}`를 이용해 문자열 안에 값이나 표현식을 삽입한다.
9. `||`는 Falsy 전체를, `??`는 `null`과 `undefined`만 값의 부재로 판단한다.
10. 옵셔널 체이닝은 선택적으로 존재하는 속성이나 함수에 안전하게 접근한다.
11. 객체 구조 분해는 속성 이름, 배열 구조 분해는 요소 위치를 기준으로 값을 꺼낸다.
12. 스프레드는 값을 펼치고 레스트는 남은 값을 모은다.

---

## 🤔 가장 헷갈렸던 점

**1. 객체 리터럴과 JSON은 왜 비슷해 보일까?**

JSON이 자바스크립트 객체 표기법의 형태에서 영향을 받았기 때문에 모양이 비슷하다. 하지만 객체는 프로그램 안에서 사용하는 실제 값이고, JSON은 데이터를 교환하기 위한 문자열 형식이다.

```javascript
const object = { name: "아메리카노" };
const json = '{"name":"아메리카노"}';

console.log(typeof object); // object
console.log(typeof json);   // string
```

**2. `const` 객체는 왜 내부 값을 바꿀 수 있을까?**

`const`는 객체를 얼리는 문법이 아니라 변수의 재할당을 막는 문법이다. 변수가 가리키는 객체를 다른 객체로 교체할 수는 없지만, 같은 객체의 속성은 변경할 수 있다.

**3. 클로저는 단순히 함수 안에 함수를 작성하는 것일까?**

내부 함수를 만들었다는 사실만으로 핵심을 설명할 수는 없다. 내부 함수가 외부 렉시컬 환경의 변수를 참조하고, 그 참조가 이후에도 유지된다는 점이 중요하다.

**4. `for...in`과 `for...of`는 어떻게 기억할까?**

- `for...in`: 객체 **안(in)의 키**를 확인한다.
- `for...of`: 컬렉션을 구성하는 값 **of**를 하나씩 꺼낸다.

무조건적인 암기보다 “지금 필요한 것이 키인지 값인지”부터 판단하는 것이 정확하다.

**5. 스프레드와 레스트는 왜 같은 기호를 사용할까?**

`...` 뒤에 있는 값을 어느 문맥에서 사용하는지에 따라 역할이 달라진다.

- 값을 사용하는 위치에서는 묶음을 **펼친다**.
- 값을 받는 위치에서는 나머지를 **모은다**.

```javascript
fn(...numbers);          // 호출부: 펼치기
function fn(...numbers) // 선언부: 모으기
```

---

## ✍️ 오늘 느낀 점

오늘 수업을 통해 자바스크립트의 ES6 문법은 단순히 기존 코드를 짧게 줄이기 위해 만들어진 문법이 아니라는 점을 알게 되었다.

이전에는 객체의 속성을 여러 번 작성하거나 문자열을 `+`로 연결해도 결과만 정상적으로 나오면 된다고 생각했다. 하지만 구조 분해와 템플릿 리터럴을 사용하니 코드가 어떤 값을 필요로 하고 무엇을 출력하려는지가 훨씬 분명하게 보였다.

특히 `||`와 `??`의 차이가 인상 깊었다. 두 문법 모두 기본값을 설정하는 것처럼 보이지만, `0`, 빈 문자열, `false`를 정상적인 값으로 인정할 것인지에 따라 결과가 달라졌다. 짧아 보이는 연산자 하나에도 데이터의 의미에 대한 판단이 들어 있다는 점을 기억해야겠다.

클로저는 처음에는 “함수 안에 있는 함수” 정도로만 느껴졌지만, 실제 핵심은 내부 함수가 외부 함수의 변수를 기억하고 계속 사용할 수 있다는 것이었다. 앞으로 이벤트 핸들러와 비동기 코드를 공부할 때도 함수가 어떤 변수를 참조하고 그 변수가 얼마나 오래 유지되는지를 함께 살펴봐야겠다.

오늘 배운 문법을 무조건 많이 사용하는 것보다, 기존 코드에서 반복되는 접근이나 불분명한 기본값 처리를 발견했을 때 왜 이 문법이 필요한지 판단하며 적용하는 연습을 해야겠다.

---

## 📎 출처

본 글은 **뉴렉처(Newlecture) 박용우 강사님의 강의 자료 및 수업 내용**을 바탕으로 학습한 내용을 정리한 글입니다.

- 뉴렉처: [https://www.newlecture.com](https://www.newlecture.com)
