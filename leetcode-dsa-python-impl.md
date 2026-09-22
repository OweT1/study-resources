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
- [19. Binary Indexed Tree (Fenwick Tree)](#19-binary-indexed-tree-fenwick-tree)
- [Complexity Summary](#complexity-summary)
- [Practice Resources](#practice-resources)

---

## 1. Counter

```python
from collections import Counter

test_string = "abc"
test_counter = Counter(test_string)
test_counter  # Output: {"a": 1, "b": 1, "c": 1}
```

**Complexity**

- Time: O(n), where n is the length of the input iterable.
- Space: O(k), where k is the number of distinct elements.

---

## 2. Default Dictionary

```python
from collections import defaultdict

test_string = "abca"
test_counter = defaultdict(int)
for c in test_string:
    test_counter[c] += 1

test_counter  # Output: {"a": 2, "b": 1, "c": 1}
```

**Complexity**

- Time: O(n), where n is the length of the input iterable.
- Space: O(k), where k is the number of distinct keys.

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

**Complexity**

- Time: O(log n) per push/pop; O(n log n) to push and pop all n items.
- Space: O(n) for the heap.

**Relevant LeetCode Questions**

- Easy: [703. Kth Largest Element in a Stream](https://leetcode.com/problems/kth-largest-element-in-a-stream/)
- Medium: [621. Task Scheduler](https://leetcode.com/problems/task-scheduler/)
- Hard: [2402. Meeting Rooms III](https://leetcode.com/problems/meeting-rooms-iii/)

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
    return -1  # not found
```

**Complexity**

- Time: O(log n), where n is the length of `nums`.
- Space: O(1) iterative (an equivalent recursive version would be O(log n) due to the call stack).

**Relevant LeetCode Questions**

- Medium: [33. Search in Rotated Sorted Array](https://leetcode.com/problems/search-in-rotated-sorted-array/)
- Medium: [875. Koko Eating Bananas](https://leetcode.com/problems/koko-eating-bananas/)
- Medium: [981. Time Based Key-Value Store](https://leetcode.com/problems/time-based-key-value-store/)
- Hard: [4. Median of Two Sorted Arrays](https://leetcode.com/problems/median-of-two-sorted-arrays/)

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

**Complexity**

- Time: O(L) per `insert`/`search`/`starts_with` call, where L is the length of the word or prefix.
- Space: O(N · L) in the worst case across all inserted words (N = number of words, L = average word length); each node additionally holds O(alphabet size) children.

**Relevant LeetCode Questions**

- Medium: [208. Implement Trie (Prefix Tree)](https://leetcode.com/problems/implement-trie-prefix-tree/)
- Medium: [211. Design Add and Search Words Data Structure](https://leetcode.com/problems/design-add-and-search-words-data-structure/)
- Medium: [212. Word Search II](https://leetcode.com/problems/word-search-ii/)

---

## 6. Union Find (Disjoint Set Union)

```python

class UnionFind:

    def __init__(self, n: int):
        self.parents = list(range(n))
        self.size = [1] * n

    def find(self, x: int) -> int:
        if self.parents[x] == x:
          return x

        self.parents[x] = self.find(self.parents[x]) # full path compression
        return self.parents[x]

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

**Complexity**

- Time: O(α(n)) amortized per `find`/`union` call (inverse Ackermann, effectively constant) thanks to path compression combined with union by size.
- Space: O(n) for the `parents` and `size` arrays.

**Relevant LeetCode Questions**

- Medium: [684. Redundant Connection](https://leetcode.com/problems/redundant-connection/)
- Medium: [721. Accounts Merge](https://leetcode.com/problems/accounts-merge/)

---

## 7. Prim's & Kruskal's (Minimum Spanning Tree)

```python

import heapq

def prims_mst(n: int, edges: list[tuple], start_node: int) -> list[tuple]:
    mst = []
    visited = {start_node}
    neighbours = {i: [] for i in range(n)}

    for u, v, w in edges:  # assume edge is (u, v, w)
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
    uf = UnionFind(n)  # from section 6
    sorted_edges = sorted(edges, key=lambda x: x[2])  # assume edge is (u, v, w)
    mst = []

    for u, v, w in sorted_edges:
        if uf.union(u, v):
            mst.append((u, v, w))
            if len(mst) == n - 1:
                break

    return mst
```

**Complexity**

- Prim's: Time O(E log E) (each edge may be pushed/popped from the heap once); Space O(V + E) for the adjacency map and heap.
- Kruskal's: Time O(E log E), dominated by sorting the edges (the DSU operations add only O(E · α(V))); Space O(V + E) — O(V) for the DSU arrays, O(E) for the edge list.

**Relevant LeetCode Questions**

- Medium: [1584. Min Cost to Connect All Points](https://leetcode.com/problems/min-cost-to-connect-all-points/)
- Hard: [1489. Find Critical and Pseudo-Critical Edges in Minimum Spanning Tree](https://leetcode.com/problems/find-critical-and-pseudo-critical-edges-in-minimum-spanning-tree/)
- Premium: [1135. Connecting Cities With Minimum Cost](https://leetcode.com/problems/connecting-cities-with-minimum-cost/)
- Premium: [1168. Optimize Water Distribution in a Village](https://leetcode.com/problems/optimize-water-distribution-in-a-village/)

---

## 8. Dijkstra's Algorithm

```python
import heapq
import math

def dijkstra(n: int, edges: list[tuple], src: int) -> list[int]:
    dists = [math.inf] * n
    dists[src] = 0
    neighbours = {}

    for u, v, w in edges:  # assume edge is (u, v, w)
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

**Complexity**

- Time: O(E log V) using a binary heap (each edge can trigger a heap push/pop, each O(log V)).
- Space: O(V + E) for the adjacency structure, distances array, and heap.

**Relevant LeetCode Questions**

- Medium: [743. Network Delay Time](https://leetcode.com/problems/network-delay-time/)
- Medium: [787. Cheapest Flights Within K Stops](https://leetcode.com/problems/cheapest-flights-within-k-stops/)
- Hard: [778. Swim in Rising Water](https://leetcode.com/problems/swim-in-rising-water/)

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

**Complexity**

- Time: O(n), a single pass over `nums`.
- Space: O(1), only two running variables are kept.

**Relevant LeetCode Questions**

- Medium: [53. Maximum Subarray](https://leetcode.com/problems/maximum-subarray/)
- Medium: [152. Maximum Product Subarray](https://leetcode.com/problems/maximum-product-subarray/)
- Medium: [918. Maximum Sum Circular Subarray](https://leetcode.com/problems/maximum-sum-circular-subarray/)

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

**Complexity**

- Time: O(n) for both patterns — the fast pointer visits each node a bounded number of times, and in the cycle case the pointers meet within one lap of the cycle.
- Space: O(1), only two pointers are used.

**Relevant LeetCode Questions**

- Easy: [141. Linked List Cycle](https://leetcode.com/problems/linked-list-cycle/)
- Easy: [202. Happy Number](https://leetcode.com/problems/happy-number/)
- Easy: [876. Middle of the Linked List](https://leetcode.com/problems/middle-of-the-linked-list/)
- Medium: [142. Linked List Cycle II](https://leetcode.com/problems/linked-list-cycle-ii/)
- Medium: [287. Find the Duplicate Number](https://leetcode.com/problems/find-the-duplicate-number/)

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

**Complexity**

- Time: O(V + E), where V is the number of nodes and E is the number of edges.
- Space: O(V) for the visited set and queue.

**Relevant LeetCode Questions**

- Medium: [102. Binary Tree Level Order Traversal](https://leetcode.com/problems/binary-tree-level-order-traversal/)
- Medium: [200. Number of Islands](https://leetcode.com/problems/number-of-islands/)
- Medium: [542. 01 Matrix](https://leetcode.com/problems/01-matrix/)
- Medium: [994. Rotting Oranges](https://leetcode.com/problems/rotting-oranges/)
- Medium: [1091. Shortest Path in Binary Matrix](https://leetcode.com/problems/shortest-path-in-binary-matrix/)
- Hard: [127. Word Ladder](https://leetcode.com/problems/word-ladder/)

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

**Complexity**

- Time: O(V + E), where V is the number of nodes and E is the number of edges.
- Space: O(V) — recursion stack (or explicit stack) plus the `visited` set.

**Relevant LeetCode Questions**

- Easy: [543. Diameter of Binary Tree](https://leetcode.com/problems/diameter-of-binary-tree/)
- Medium: [130. Surrounded Regions](https://leetcode.com/problems/surrounded-regions/)
- Medium: [133. Clone Graph](https://leetcode.com/problems/clone-graph/)
- Medium: [494. Target Sum](https://leetcode.com/problems/target-sum/)
- Medium: [690. Employee Importance](https://leetcode.com/problems/employee-importance/)
- Hard: [124. Binary Tree Maximum Path Sum](https://leetcode.com/problems/binary-tree-maximum-path-sum/)
- Premium: [690. Employee Importance](https://leetcode.com/problems/employee-importance/)

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

**Complexity**

- Time: O(2^n · n) — there are 2^n subsets, and each one costs O(n) to copy into `results` (often written as O(2^n) if the copy cost is ignored).
- Space: O(n) for the recursion depth / `path`, excluding the output; O(2^n · n) if the stored output is counted.

**Relevant LeetCode Questions**

- Medium: [39. Combination Sum](https://leetcode.com/problems/combination-sum/)
- Medium: [46. Permutations](https://leetcode.com/problems/permutations/)
- Medium: [78. Subsets](https://leetcode.com/problems/subsets/)
- Medium: [79. Word Search](https://leetcode.com/problems/word-search/)
- Medium: [90. Subsets II](https://leetcode.com/problems/subsets-ii/)
- Medium: [131. Palindrome Partitioning](https://leetcode.com/problems/palindrome-partitioning/)
- Medium: [216. Combination Sum III](https://leetcode.com/problems/combination-sum-iii/)
- Hard: [51. N-Queens](https://leetcode.com/problems/n-queens/)

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
- 1D DP above: `dp[i]` is the Fibonacci value at `i`, derived from the two previous states.
- 2D DP above: `dp[r][c]` is the minimum cost to reach cell `(r, c)` from the top-left, using only up/left moves.
- In actual LeetCode problems, the most important part is usually finding the correct **state definition and transition**, not memorizing a particular code template.

**Complexity**

- 1D DP (Fibonacci-style): Time O(n); Space O(n) as written (can be reduced to O(1) by keeping only the last two values).
- 2D DP (min path sum): Time O(rows × cols); Space O(rows × cols) as written (can be reduced to O(cols) with a rolling 1D array).

**Relevant LeetCode Questions**

- Easy: [70. Climbing Stairs](https://leetcode.com/problems/climbing-stairs/)
- Easy: [121. Best Time to Buy and Sell Stock](https://leetcode.com/problems/best-time-to-buy-and-sell-stock/)
- Medium: [62. Unique Paths](https://leetcode.com/problems/unique-paths/)
- Medium: [139. Word Break](https://leetcode.com/problems/word-break/)
- Medium: [300. Longest Increasing Subsequence](https://leetcode.com/problems/longest-increasing-subsequence/)
- Medium: [322. Coin Change](https://leetcode.com/problems/coin-change/)
- Medium: [416. Partition Equal Subset Sum](https://leetcode.com/problems/partition-equal-subset-sum/)
- Medium: [494. Target Sum](https://leetcode.com/problems/target-sum/)
- Medium: [518. Coin Change II](https://leetcode.com/problems/coin-change-ii/)
- Medium: [1143. Longest Common Subsequence](https://leetcode.com/problems/longest-common-subsequence/)

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
- Each character enters and leaves the set at most once, giving O(n) time.

**Complexity**

- Time: O(n), where n is the length of `s` — each character is added and removed from `seen` at most once.
- Space: O(min(n, Σ)), where Σ is the alphabet/character-set size, for the `seen` set.

**Relevant LeetCode Questions**

- Medium: [3. Longest Substring Without Repeating Characters](https://leetcode.com/problems/longest-substring-without-repeating-characters/)
- Medium: [209. Minimum Size Subarray Sum](https://leetcode.com/problems/minimum-size-subarray-sum/)
- Medium: [424. Longest Repeating Character Replacement](https://leetcode.com/problems/longest-repeating-character-replacement/)
- Medium: [567. Permutation in String](https://leetcode.com/problems/permutation-in-string/)
- Medium: [1004. Max Consecutive Ones III](https://leetcode.com/problems/max-consecutive-ones-iii/)
- Hard: [76. Minimum Window Substring](https://leetcode.com/problems/minimum-window-substring/)

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

**Complexity**

- Time: O(V + E), where V is the number of nodes and E is the number of edges.
- Space: O(V + E) for the adjacency list, in-degree array, and queue.

**Relevant LeetCode Questions**

- Medium: [207. Course Schedule](https://leetcode.com/problems/course-schedule/)
- Medium: [210. Course Schedule II](https://leetcode.com/problems/course-schedule-ii/)
- Medium: [802. Find Eventual Safe States](https://leetcode.com/problems/find-eventual-safe-states/)
- Premium: [269. Alien Dictionary](https://leetcode.com/problems/alien-dictionary/)
- Premium: [1136. Parallel Courses](https://leetcode.com/problems/parallel-courses/)

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

**Complexity**

- Time: O(n) — amortized, since each index is pushed and popped at most once despite the nested `while` loop.
- Space: O(n) for the stack and the result array.

**Relevant LeetCode Questions**

- Easy: [496. Next Greater Element I](https://leetcode.com/problems/next-greater-element-i/)
- Medium: [739. Daily Temperatures](https://leetcode.com/problems/daily-temperatures/)
- Medium: [901. Online Stock Span](https://leetcode.com/problems/online-stock-span/)
- Hard: [42. Trapping Rain Water](https://leetcode.com/problems/trapping-rain-water/)
- Hard: [84. Largest Rectangle in Histogram](https://leetcode.com/problems/largest-rectangle-in-histogram/)

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

**Complexity**

- Time: O(n log n), dominated by the initial sort; the merge pass itself is O(n).
- Space: O(n) for the sorted copy and the output list (sorting itself typically adds O(log n) to O(n) extra space, depending on implementation).

**Relevant LeetCode Questions**

- Medium: [56. Merge Intervals](https://leetcode.com/problems/merge-intervals/)
- Medium: [57. Insert Interval](https://leetcode.com/problems/insert-interval/)
- Medium: [435. Non-overlapping Intervals](https://leetcode.com/problems/non-overlapping-intervals/)
- Medium: [1288. Remove Covered Intervals](https://leetcode.com/problems/remove-covered-intervals/)
- Premium: [252. Meeting Rooms](https://leetcode.com/problems/meeting-rooms/)
- Premium: [253. Meeting Rooms II](https://leetcode.com/problems/meeting-rooms-ii/)

---

## 19. Binary Indexed Tree (Fenwick Tree)

```python
class BIT:
    """1-indexed Binary Indexed Tree (Fenwick Tree) for prefix sums."""

    def __init__(self, n: int):
        self.n = n
        self.tree = [0] * (n + 1)

    def update(self, i: int, delta: int) -> None:
        """Add `delta` to the value at index i (1-indexed)."""
        while i <= self.n:
            self.tree[i] += delta
            i += i & (-i)  # move to next index that covers i

    def prefix_sum(self, i: int) -> int:
        """Return sum of values in range [1, i] (1-indexed, inclusive)."""
        total = 0
        while i > 0:
            total += self.tree[i]
            i -= i & (-i)  # move to parent range
        return total

    def range_sum(self, l: int, r: int) -> int:
        """Return sum of values in range [l, r] (1-indexed, inclusive)."""
        return self.prefix_sum(r) - self.prefix_sum(l - 1)

    @classmethod
    def from_array(cls, nums: list[int]) -> "BIT":
        """Build a BIT from a 0-indexed array in O(n) time."""
        bit = cls(len(nums))
        for i, num in enumerate(nums, start=1):
            bit.tree[i] += num
            j = i + (i & (-i))
            if j <= bit.n:
                bit.tree[j] += bit.tree[i]
        return bit
```

**Implementation notes**

- A BIT (a.k.a. Fenwick Tree) stores partial sums indexed by the lowest set bit of `i`, using `i & (-i)` to isolate it. This lets both `update` and `prefix_sum` walk O(log n) nodes instead of touching every element.
- The tree is conventionally **1-indexed** — index 0 is unused — because `i & (-i)` would be `0` for `i = 0`, causing an infinite loop.
- `update(i, delta)` climbs **up** the tree, adding `delta` to every node whose range covers index `i`, by repeatedly adding the lowest set bit (`i += i & (-i)`).
- `prefix_sum(i)` climbs **down**, accumulating the value at each node that partially covers `[1, i]`, by repeatedly removing the lowest set bit (`i -= i & (-i)`).
- `range_sum(l, r)` follows from `prefix_sum(r) - prefix_sum(l - 1)`, the same idea as a 1D prefix-sum array, but each side supports O(log n) point updates.
- `from_array` builds the whole tree in O(n) by pushing each node's accumulated value up to its immediate parent, instead of calling `update` n times (which would cost O(n log n)).
- BITs are the go-to structure when you need **both** point updates and prefix/range sum queries efficiently; if updates are range-based instead of point-based, a variant using two BITs (or a segment tree with lazy propagation) is typically used instead.

**Complexity**

- Time: O(log n) per `update` and per `prefix_sum`/`range_sum` query; O(n) to build via `from_array` (O(n log n) if built by calling `update` n times).
- Space: O(n) for the `tree` array.

**Relevant LeetCode Questions**

- Medium: [307. Range Sum Query - Mutable](https://leetcode.com/problems/range-sum-query-mutable/)
- Medium: [1409. Queries on a Permutation With Key](https://leetcode.com/problems/queries-on-a-permutation-with-key/)
- Hard: [315. Count of Smaller Numbers After Self](https://leetcode.com/problems/count-of-smaller-numbers-after-self/)
- Hard: [327. Count of Range Sum](https://leetcode.com/problems/count-of-range-sum/)
- Hard: [493. Reverse Pairs](https://leetcode.com/problems/reverse-pairs/)

---

## Complexity Summary

| #   | Pattern                   | Time                  | Space                 |
| --- | ------------------------- | --------------------- | --------------------- |
| 1   | Counter                   | O(n)                  | O(k)                  |
| 2   | Default Dictionary        | O(n)                  | O(k)                  |
| 3   | Heaps / Priority Queue    | O(n log n)            | O(n)                  |
| 4   | Binary Search             | O(log n)              | O(1)                  |
| 5   | Trie                      | O(L) per op           | O(N · L)              |
| 6   | Union Find                | O(α(n)) per op        | O(n)                  |
| 7   | Prim's / Kruskal's (MST)  | O(E log E)            | O(V + E)              |
| 8   | Dijkstra's Algorithm      | O(E log V)            | O(V + E)              |
| 9   | Kadane's Algorithm        | O(n)                  | O(1)                  |
| 10  | Slow and Fast Pointer     | O(n)                  | O(1)                  |
| 11  | BFS                       | O(V + E)              | O(V)                  |
| 12  | DFS                       | O(V + E)              | O(V)                  |
| 13  | Backtracking (subsets)    | O(2^n · n)            | O(n)                  |
| 14  | Dynamic Programming       | O(n) / O(rows × cols) | O(n) / O(rows × cols) |
| 15  | Sliding Window            | O(n)                  | O(min(n, Σ))          |
| 16  | Topological Sort (Kahn's) | O(V + E)              | O(V + E)              |
| 17  | Monotonic Stack           | O(n)                  | O(n)                  |
| 18  | Intervals (Merge)         | O(n log n)            | O(n)                  |
| 19  | Binary Indexed Tree       | O(log n) per op       | O(n)                  |

---

## Practice Resources

To get started with leetcode-style preparation, check out the following resources:

- Neetcode 150 (https://neetcode.io/practice/practice/neetcode150 / https://leetcode.com/problem-list/plakya4j/)
- Grind 75 (https://www.techinterviewhandbook.org/grind75/ / https://leetcode.com/problem-list/rab78cw1/)
- Contest Questions with rating (https://zerotrac.github.io/leetcode_problem_rating/#/)
