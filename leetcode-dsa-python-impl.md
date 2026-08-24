**# LeetCode DSA Python Implementations**

---

## Table of Contents

- [Table of Contents](#table-of-contents)
- [1. Counter](#1-counter)
- [2. Default Dictionary](#2-default-dictionary)
- [3. Heaps / Priority Queue](#3-heaps--priority-queue)
- [4. Binary Search](#4-binary-search)
- [5. Trie](#5-trie)
- [6. Union Find (Disjoint Set Union)](#6-union-find-disjoint-set-union)
- [7. Prim's \& Kruskal's (Minimum Spanning Tree)](#7-prims--kruskals-minimum-spanning-tree)
- [8. Dijkstra's Algorithm](#8-dijkstras-algorithm)
- [9. Kadane's Algorithm](#9-kadanes-algorithm)
- [10. Slow and Fast Pointer](#10-slow-and-fast-pointer)
- [11. BFS (Breadth-First Search)](#11-bfs-breadth-first-search)
- [12. DFS (Depth-First Search)](#12-dfs-depth-first-search)
- [13. Backtracking](#13-backtracking)
- [14. Dynamic Programming](#14-dynamic-programming)
- [15. Sliding Window](#15-sliding-window)
- [16. Topological Sort (Kahn's Algorithm / BFS-based)](#16-topological-sort-kahns-algorithm--bfs-based)
- [17. Monotonic Stack](#17-monotonic-stack)
- [18. Intervals (Merge / Sort-based)](#18-intervals-merge--sort-based)

---

## 1. Counter

```python
from collections import Counter

test_string = "abc"
test_counter = Counter(test_string)
test_counter  # Output: {"a": 1, "b": 1, "c": 1}
```

---

## 2. Default Dictionary

```python
from collections import defaultdict

test_string = "abca"
test_counter = defaultdict(int)
for c in test_string:
    test_counter[c] += 1

test_counter  # Output: {"a": 2, "b": 1, "c": 1}
```

---

## 3. Heaps / Priority Queue

```python
# Min-heap

import heapq

pq = []
items = [(0, 1), (2, 4), (2, 1), (1, 2), (0, 0)]

for item in items:
    heapq.heappush(pq, item)

while pq:
    heapq.heappop(pq)

# Output order: (0,0), (0,1), (1,2), (2,1), (2,4)

# Max-heap - negate values instead:

pq = []
for item in items:
    heapq.heappush(pq, (-item[0], -item[1], item))

while pq:
    heapq.heappop(pq)
```

**Relevant LeetCode Questions**

- [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/)
- [2402. Meeting Rooms III](https://leetcode.com/problems/meeting-rooms-iii/)

---

## 4. Binary Search

Take note of not found scenario.

```python
def binary_search(nums: list[int], target: int) -> int:
    l, r = 0, len(nums) - 1

    while l <= r:
        m = l + ((r - l) >> 1)
        if nums[m] == target:
            return m
        elif nums[m] > target:
            r = m - 1
        else:
            l = m + 1
    return -1  # not found
```

**Relevant LeetCode Questions**

- [33. Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/)
- [875. Koko Eating Bananas (binary search on answer)](https://leetcode.com/problems/koko-eating-bananas/)

---

## 5. Trie

```python
class TrieNode:
    def __init__(self):
        self.children = {}
        self.is_end = False

class Trie:
    def __init__(self):
        self.root = TrieNode()

    def insert(self, word: str) -> None:
        tmp = self.root
        for c in word:
            if c not in tmp.children:
                tmp.children[c] = TrieNode()
            tmp = tmp.children[c]
        tmp.is_end = True

    def search(self, word: str) -> bool:
        tmp = self.root
        for c in word:
            if c not in tmp.children:
                return False
            tmp = tmp.children[c]
        return tmp.is_end

    def starts_with(self, prefix: str) -> bool:
        tmp = self.root
        for c in prefix:
            if c not in tmp.children:
                return False
            tmp = tmp.children[c]
        return True
```

**Relevant LeetCode Questions**

- [208. Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/)
- [211. Design Add and Search Words Data Structure](https://leetcode.com/problems/design-add-and-search-words-data-structure/)
- [212. Word Search II](https://leetcode.com/problems/word-search-ii/)

---

## 6. Union Find (Disjoint Set Union)

```python

class UnionFind:

    def __init__(self, n: int):
        self.parents = list(range(n))
        self.size = [1] * n

    def find(self, x: int) -> int:
        while x != self.parents[x]:
            self.parents[x] = self.parents[self.parents[x]]  # path halving
            x = self.parents[x]
        return x

    def union(self, x: int, y: int) -> bool:
        p_x, p_y = self.find(x), self.find(y)
        if p_x == p_y: return False

        if self.size[p_x] > self.size[p_y]:
            self.parents[p_y] = p_x
            self.size[p_x] += self.size[p_y]
        else:
            self.parents[p_x] = p_y
            self.size[p_y] += self.size[p_x]

        return True
```

**Relevant LeetCode Questions**

- [684. Redundant Connection](https://leetcode.com/problems/redundant-connection/)
- [721. Accounts Merge](https://leetcode.com/problems/accounts-merge/)

---

## 7. Prim's & Kruskal's (Minimum Spanning Tree)

```python

import heapq

def prims_mst(n: int, edges: list[tuple], start_node: int) -> list[tuple]:
    mst = []
    visited = {start_node}
    neighbours = {i: [] for i in range(n)}

    for u, v, w in edges:  # assume edge is (u, v, w)
        neighbours[u].append((w, u, v))
        neighbours[v].append((w, v, u))

    pq = neighbours[start_node][:]
    heapq.heapify(pq)

    while pq and len(visited) < n:
        w, u, v = heapq.heappop(pq)
        if v in visited:
            continue

        visited.add(v)
        mst.append((u, v, w))

        for edge in neighbours.get(v, []):
            if edge[2] not in visited:
                heapq.heappush(pq, edge)

    return mst

def kruskals_mst(n: int, edges: list[tuple]) -> list[tuple]:
    uf = UnionFind(n)  # from section 6
    sorted_edges = sorted(edges, key=lambda x: x[2])  # assume edge is (u, v, w)
    mst = []

    for u, v, w in sorted_edges:
        if uf.union(u, v):
            mst.append((u, v, w))
            if len(mst) == n - 1:
                break

    return mst
```

**Relevant LeetCode Questions**

- [1135. Connecting Cities With Minimum Cost](https://leetcode.com/problems/connecting-cities-with-minimum-cost/)
- [1168. Optimize Water Distribution in a Village](https://leetcode.com/problems/optimize-water-distribution-in-a-village/)
- [1489. Find Critical and Pseudo-Critical Edges in Minimum Spanning Tree](https://leetcode.com/problems/find-critical-and-pseudo-critical-edges-in-minimum-spanning-tree/)
- [1584. Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/)

---

## 8. Dijkstra's Algorithm

```python
import heapq
import math

def dijkstra(n: int, edges: list[tuple], src: int) -> list[int]:
    dists = [math.inf] * n
    dists[src] = 0
    neighbours = {}

    for u, v, w in edges:  # assume edge is (u, v, w)
        if u not in neighbours:
            neighbours[u] = {}
        neighbours[u][v] = w

    pq = [(0, src)]
    while pq:
        dist, node = heapq.heappop(pq)
        if node not in neighbours or dist > dists[node]:
            continue

        for nb, w in neighbours[node].items():
            if dist + w < dists[nb]:
                dists[nb] = dist + w
                heapq.heappush(pq, (dists[nb], nb))
    return dists

```

**Relevant LeetCode Questions**

- [743. Network Delay Time](https://leetcode.com/problems/network-delay-time/)
- [778. Swim in Rising Water (https://leetcode.com/problems/swim-in-rising-water/)](https://leetcode.com/problems/swim-in-rising-water/)
- [787. Cheapest Flights Within K Stops (variant / modified Dijkstra or BFS)](https://leetcode.com/problems/cheapest-flights-within-k-stops/)

---

## 9. Kadane's Algorithm

```python

def kadane(nums: list[int]) -> int:
    curr = res = nums[0]
    for num in nums[1:]:
        curr = max(num, curr + num)
        res = max(res, curr)
    return res

```

**Relevant LeetCode Questions**

- [53. Maximum Subarray](https://leetcode.com/problems/maximum-subarray/)
- [152. Maximum Product Subarray (modified Kadane)](https://leetcode.com/problems/maximum-product-subarray/)

---

## 10. Slow and Fast Pointer

```python

from __future__ import annotations

class Node:
    def __init__(self, val: int, next: Node | None = None):
        self.val = val
        self.next = next

# Pattern A: find the middle node (LC 876)

def find_middle(head: Node) -> Node:
    slow = fast = head
    while fast and fast.next:
        slow = slow.next
        fast = fast.next.next
    return slow

# Pattern B: cycle detection (LC 141)

def has_cycle(head: Node) -> bool:
    slow = fast = head
    while fast and fast.next:
        slow = slow.next
        fast = fast.next.next
        if slow == fast:
            return True
    return False

```

**Relevant LeetCode Questions**

- [141. Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/)
- [142. Linked List Cycle II](https://leetcode.com/problems/linked-list-cycle-ii/)
- [202. Happy Number](https://leetcode.com/problems/happy-number/)
- [876. Middle of the Linked List](https://leetcode.com/problems/middle-of-the-linked-list/)

---

## 11. BFS (Breadth-First Search)

```python
from collections import deque

def bfs(start: int, graph: dict[int, list[int]]) -> list[int]:
    """Return BFS traversal order from start."""
    visited = {start}
    dq = deque([start])
    order = []

    while dq:
        node = dq.popleft()
        order.append(node)

        for nb in graph.get(node, []):
            if nb not in visited:
                visited.add(nb)
                dq.append(nb)

    return order
```

**Implementation notes**

- `deque.popleft()` gives O(1) queue removal; using a normal list with `pop(0)` would be O(n).
- Mark nodes visited **when enqueuing**, not when dequeuing. This prevents the same node from being added to the queue multiple times.
- This is the graph form of BFS. For a grid, the same queue pattern applies, but you additionally check bounds and neighboring cells.
- Time: O(V + E); space: O(V).

**Relevant LeetCode Questions**

- [102. Binary Tree Level Order Traversal](https://leetcode.com/problems/binary-tree-level-order-traversal/)
- [127. Word Ladder](https://leetcode.com/problems/word-ladder/)
- [200. Number of Islands](https://leetcode.com/problems/number-of-islands/)
- [207. Course Schedule](https://leetcode.com/problems/course-schedule/)
- [542. 01 Matrix](https://leetcode.com/problems/01-matrix/)
- [994. Rotting Oranges](https://leetcode.com/problems/rotting-oranges/)
- [1091. Shortest Path in Binary Matrix](https://leetcode.com/problems/shortest-path-in-binary-matrix/)

---

## 12. DFS (Depth-First Search)

```python
def dfs(
    node: int,
    graph: dict[int, list[int]],
    visited: set[int],
    order: list[int],
) -> None:
    """Recursive DFS."""
    if node in visited:
        return

    visited.add(node)
    order.append(node)

    for nb in graph.get(node, []):
        if nb not in visited:
            dfs(nb, graph, visited, order)


def dfs_iterative(start: int, graph: dict[int, list[int]]) -> list[int]:
    """Iterative DFS using an explicit stack."""
    visited = {start}
    stack = [start]
    order = []

    while stack:
        node = stack.pop()
        order.append(node)

        for nb in reversed(graph.get(node, [])):
            if nb not in visited:
                visited.add(nb)
                stack.append(nb)

    return order
```

**Implementation notes**

- The original recursive template had `continue` outside a loop; that is a syntax error. Use `return` when the current node should not be processed.
- Recursive DFS needs a `visited` set to avoid cycles.
- Iterative DFS replaces the call stack with an explicit Python list used as a stack (`append`/`pop`).
- Marking nodes visited when pushing them prevents duplicates.
- Time: O(V + E); space: O(V), including recursion/stack and `visited`.

**Relevant LeetCode Questions**

- [200. Number of Islands](https://leetcode.com/problems/number-of-islands/)
- [133. Clone Graph](https://leetcode.com/problems/clone-graph/)
- [130. Surrounded Regions](https://leetcode.com/problems/surrounded-regions/)
- [494. Target Sum](https://leetcode.com/problems/target-sum/)
- [543. Diameter of Binary Tree](https://leetcode.com/problems/diameter-of-binary-tree/)
- [690. Employee Importance](https://leetcode.com/problems/employee-importance/)

---

## 13. Backtracking

```python
def backtrack_subsets(nums: list[int]) -> list[list[int]]:
    """Return all subsets of nums."""
    results = []
    path = []

    def backtrack(i: int) -> None:
        if i == len(nums):
            results.append(path.copy())
            return

        # Choice 1: do not take nums[i].
        backtrack(i + 1)

        # Choice 2: take nums[i].
        path.append(nums[i])
        backtrack(i + 1)
        path.pop()

    backtrack(0)
    return results
```

**Implementation notes**

- Backtracking is a DFS over a **decision tree**: at each position, choose one of the available options, recurse, then undo the choice.
- `path` is mutable and reused. `path.pop()` is the critical "backtrack" step that restores the state before exploring the next branch.
- `path.copy()` is required when storing a result; otherwise every entry in `results` would refer to the same mutable list.
- The original template used `bfs()` even though this is a depth-first recursive search, and its base case/return value did not actually enumerate the choices.
- This example generates `2^n` subsets, so the output itself is already O(2^n).

**Relevant LeetCode Questions**

- [39. Combination Sum](https://leetcode.com/problems/combination-sum/)
- [46. Permutations](https://leetcode.com/problems/permutations/)
- [51. N-Queens](https://leetcode.com/problems/n-queens/)
- [78. Subsets](https://leetcode.com/problems/subsets/)
- [79. Word Search](https://leetcode.com/problems/word-search/)
- [90. Subsets II](https://leetcode.com/problems/subsets-ii/)
- [131. Palindrome Partitioning](https://leetcode.com/problems/palindrome-partitioning/)
- [216. Combination Sum III](https://leetcode.com/problems/combination-sum-iii/)

---

## 14. Dynamic Programming

A useful way to think about DP is:

1. Define what `dp[i]` or `dp[i][j]` means.
2. Identify the recurrence/transition.
3. Define base cases.
4. Choose an evaluation order so dependencies are already known.
5. Return the state representing the final answer.

```python
# 1D DP example: Fibonacci-style recurrence.
def dp_1d_template(n: int) -> int:
    if n <= 1:
        return n

    dp = [0] * (n + 1)
    dp[0] = 0
    dp[1] = 1

    for i in range(2, n + 1):
        dp[i] = dp[i - 1] + dp[i - 2]

    return dp[n]


# 2D DP example: minimum path sum in a grid.
def dp_2d_template(grid: list[list[int]]) -> int:
    rows, cols = len(grid), len(grid[0])
    dp = [[0] * cols for _ in range(rows)]

    dp[0][0] = grid[0][0]

    for r in range(1, rows):
        dp[r][0] = dp[r - 1][0] + grid[r][0]

    for c in range(1, cols):
        dp[0][c] = dp[0][c - 1] + grid[0][c]

    for r in range(1, rows):
        for c in range(1, cols):
            dp[r][c] = grid[r][c] + min(
                dp[r - 1][c],
                dp[r][c - 1],
            )

    return dp[-1][-1]
```

**Implementation notes**

- The original stubs were intentionally too abstract to be executable because the recurrence depends on the problem. These two examples make the templates concrete while showing the two common shapes.
- 1D DP above: `dp[i]` is the Fibonacci value at `i`, derived from the two previous states. Time O(n), space O(n).
- 2D DP above: `dp[r][c]` is the minimum cost to reach cell `(r, c)` from the top-left, using only up/left moves. Time O(rows × cols), space O(rows × cols).
- In actual LeetCode problems, the most important part is usually finding the correct **state definition and transition**, not memorizing a particular code template.

**Relevant LeetCode Questions**

- [70. Climbing Stairs](https://leetcode.com/problems/climbing-stairs/)
- [62. Unique Paths](https://leetcode.com/problems/unique-paths/)
- [121. Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/)
- [139. Word Break](https://leetcode.com/problems/word-break/)
- [300. Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/)
- [322. Coin Change](https://leetcode.com/problems/coin-change/)
- [416. Partition Equal Subset Sum](https://leetcode.com/problems/partition-equal-subset-sum/)
- [494. Target Sum](https://leetcode.com/problems/target-sum/)
- [518. Coin Change II](https://leetcode.com/problems/coin-change-ii/)
- [1143. Longest Common Subsequence](https://leetcode.com/problems/longest-common-subsequence/)
- [1584. Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/)

---

## 15. Sliding Window

```python
def longest_unique_substring(s: str) -> int:
    """Longest substring without repeating characters."""
    l = 0
    res = 0
    seen = set()

    for r, char in enumerate(s):
        while char in seen:
            seen.remove(s[l])
            l += 1

        seen.add(char)
        res = max(res, r - l + 1)

    return res
```

**Implementation notes**

- The original template referenced an undefined `condition` and `res`, so it was not executable.
- A sliding window maintains a valid range `[l, r]`. Expand `r`; when the window becomes invalid, move `l` until the constraint is restored.
- In this example, the window must contain no duplicate characters. The `while` loop removes characters from the left until the new character can be included.
- Each character enters and leaves the set at most once, giving O(n) time and O(min(n, alphabet size)) space.

**Relevant LeetCode Questions**

- [3. Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/)
- [76. Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/)
- [209. Minimum Size Subarray Sum](https://leetcode.com/problems/minimum-size-subarray-sum/)
- [424. Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/)
- [567. Permutation in String](https://leetcode.com/problems/permutation-in-string/)
- [1004. Max Consecutive Ones III](https://leetcode.com/problems/max-consecutive-ones-iii/)

---

## 16. Topological Sort (Kahn's Algorithm / BFS-based)

```python
from collections import deque

def topo_sort(n: int, edges: list[tuple[int, int]]) -> list[int]:
    """Return a topological ordering, or [] if the graph has a cycle."""
    in_deg = [0] * n
    neighbours = [[] for _ in range(n)]

    for u, v in edges:
        neighbours[u].append(v)
        in_deg[v] += 1

    dq = deque(node for node in range(n) if in_deg[node] == 0)
    res = []

    while dq:
        node = dq.popleft()
        res.append(node)

        for nb in neighbours[node]:
            in_deg[nb] -= 1
            if in_deg[nb] == 0:
                dq.append(nb)

    return res if len(res) == n else []
```

**Implementation notes**

- Kahn's algorithm repeatedly takes a node with indegree 0, adds it to the answer, and removes its outgoing edges by decrementing neighbors' indegrees.
- The original implementation had several issues:
  - `for edge in edges` did not unpack `(u, v)`, but then referenced `v`.
  - The comment said edges were `(u, v, w)`, although topological sorting only needs `(u, v)`.
  - Initializing indegrees from `in_deg.items()` omitted isolated nodes and nodes that only appear as sources.
  - There was no cycle check.
- If fewer than `n` nodes are processed, the graph contains a directed cycle.
- Time: O(V + E); space: O(V + E).

**Relevant LeetCode Questions**

- [207. Course Schedule](https://leetcode.com/problems/course-schedule/)
- [210. Course Schedule II](https://leetcode.com/problems/course-schedule-ii/)
- [269. Alien Dictionary](https://leetcode.com/problems/alien-dictionary/)
- [802. Find Eventual Safe States](https://leetcode.com/problems/find-eventual-safe-states/)
- [1136. Parallel Courses](https://leetcode.com/problems/parallel-courses/)

---

## 17. Monotonic Stack

```python
def next_greater_elements(nums: list[int]) -> list[int]:
    """For each element, return the next greater element to its right."""
    res = [-1] * len(nums)
    stack = []  # indices; values are decreasing from bottom to top

    for i, num in enumerate(nums):
        while stack and nums[stack[-1]] < num:
            j = stack.pop()
            res[j] = num

        stack.append(i)

    return res
```

**Implementation notes**

- The stack stores **indices**, which lets us both compare values (`nums[index]`) and write the answer into the correct position.
- The stack is monotonic decreasing: before pushing `num`, pop every smaller value because `num` is its first greater value to the right.
- Each index is pushed once and popped once, so the total work is O(n), despite the nested `while` loop.
- This is the core pattern behind "next greater/smaller", daily temperatures, stock span, and histogram problems.

**Relevant LeetCode Questions**

- [42. Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/)
- [84. Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/)
- [496. Next Greater Element I](https://leetcode.com/problems/next-greater-element-i/)
- [739. Daily Temperatures](https://leetcode.com/problems/daily-temperatures/)
- [901. Online Stock Span](https://leetcode.com/problems/online-stock-span/)

---

## 18. Intervals (Merge / Sort-based)

```python
def merge_intervals(intervals: list[list[int]]) -> list[list[int]]:
    if not intervals:
        return []

    intervals = sorted(intervals)
    output = [intervals[0].copy()]

    for start, end in intervals[1:]:
        if start <= output[-1][1]:
            output[-1][1] = max(output[-1][1], end)
        else:
            output.append([start, end])

    return output
```

**Implementation notes**

- Sort intervals by start time first. After sorting, only the last merged interval can overlap the current interval.
- The original implementation used `interval[0] < output[-1][1]`. Using `<=` also merges touching intervals such as `[1, 2]` and `[2, 3]`, which is the standard interpretation for [56. Merge Intervals](https://leetcode.com/problems/merge-intervals/).
- The original version also used `pop()` unnecessarily and could mutate the caller's input through `output.append(interval)`. The implementation above copies the first interval and creates new merged intervals.
- Time: O(n log n) for sorting; merge pass is O(n). Extra space is O(n) for the output.

**Relevant LeetCode Questions**

- [56. Merge Intervals](https://leetcode.com/problems/merge-intervals/)
- [57. Insert Interval](https://leetcode.com/problems/insert-interval/)
- [252. Meeting Rooms](https://leetcode.com/problems/meeting-rooms/)
- [253. Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/)
- [435. Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/)
- [1288. Remove Covered Intervals](https://leetcode.com/problems/remove-covered-intervals/)

---
