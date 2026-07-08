# ML & LLM Interview Guide

> **How to use this guide:** Each section covers a topic area commonly tested in ML/LLM engineer interviews. Questions marked ⭐ are near-universal — expect them in almost every interview.

---

## Table of Contents

- [Part 1 — ML Foundations](#part-1--ml-foundations)
- [Part 2 — LLMs](#part-2--llms)
- [Part 3 — RAG & LLM Evaluation](#part-3--rag--llm-evaluation)
- [Part 4 — MLOps, Systems & Production](#part-4--mlops-systems--production)
- [Part 5 — Agentic Systems](#part-5--agentic-systems)

---

## Part 1 — ML Foundations

---

### Q1: Does accuracy work as a metric for imbalanced data?

**No** — accuracy is misleading on imbalanced datasets. If 95% of samples are class A and 5% are class B, a model that predicts class A for everything achieves 95% accuracy while being completely useless for detecting class B.

This is called the **accuracy paradox**. The metric hides the model's failure to learn the minority class, which is usually the class you care about most (e.g. fraud, disease, defects).

---

### Q1 Follow-up: What metrics should you use instead?

- **Precision** — of all positive predictions, how many were actually positive? Useful when false positives are costly (e.g. spam filters).
- **Recall (Sensitivity)** — of all actual positives, how many did the model catch? Critical when false negatives are costly (e.g. cancer screening).
- **F1-Score** — harmonic mean of precision and recall. Balances both when you need a single number. Use F1-macro or F1-weighted for multi-class problems.
- **AUC-ROC** — measures the model's ability to discriminate between classes across all thresholds. A score of 0.5 is random; 1.0 is perfect. Works well for binary classification on imbalanced data.
- **Matthews Correlation Coefficient (MCC)** — considered one of the best single metrics for imbalanced binary classification. It accounts for all four values in the confusion matrix (TP, TN, FP, FN).

> **Rule of thumb**: choose your metric based on what kind of error is most costly in your domain. Precision vs recall reflects a trade-off you must consciously decide.

---

### Q2: Low training loss, high validation loss — what is the problem?

This is a classic sign of **overfitting**. The model has memorised the training data — it has learned the noise and specific patterns of the training set rather than the underlying general distribution.

As a result, it performs well on training data but fails to generalise to unseen examples (the validation set).

**Common causes:**
- Model too complex
- Too few training samples
- Too many epochs
- No regularisation
- Data leakage

**Solutions:** Add regularisation (L1/L2, dropout), reduce model complexity, get more training data, use early stopping, or apply data augmentation.

---

### Q3: What is regularisation and how does it work?

Regularisation is a set of techniques that penalise model complexity during training to prevent overfitting. The idea is to add a penalty term to the loss function that discourages the model from learning overly large or complex weights.

- **L2 regularisation (Ridge)** — adds the sum of squared weights (λ∑w²) to the loss. This shrinks all weights towards zero smoothly, preventing any single feature from dominating. Common in neural networks via weight decay.
- **L1 regularisation (Lasso)** — adds the sum of absolute weights (λ∑|w|) to the loss. This can drive some weights to exactly zero, effectively performing feature selection. Useful for sparse models.
- **Dropout** — during training, randomly zeros out a fraction of neurons in each layer. Forces the network to learn redundant representations and prevents co-adaptation of neurons.
- **Early stopping** — monitor validation loss and stop training when it starts increasing, even if training loss is still decreasing.

The regularisation strength λ is a hyperparameter — too small and overfitting persists; too large and the model underfits.

---

### Q4: Problems with high-dimensional feature spaces (e.g. bag-of-words)

- **Curse of dimensionality** — as dimensions increase, data becomes increasingly sparse. The volume of space grows exponentially, so training points are far apart and the model struggles to generalise.
- **Overfitting** — with thousands of features and limited data, models can latch onto spurious correlations in the training set.
- **Computational cost** — training time and memory usage scale badly with feature count.
- **Multicollinearity** — many words may be correlated (synonyms, related terms), making it harder for models to isolate signal.
- **Sparsity** — most documents contain only a small fraction of the vocabulary, producing very sparse feature vectors.

---

### Q4 Follow-up: How do you select features for your model?

- **Filter methods** — rank features independently of the model using statistical tests: chi-squared, mutual information, ANOVA F-test, or correlation with the target. Fast but ignores feature interactions.
- **Wrapper methods** — use model performance to evaluate subsets. Forward selection (start with no features, add the best one at each step), backward elimination (start with all, remove the worst). Expensive but accounts for interactions.
- **Embedded methods** — feature selection happens during training. L1 (Lasso) regularisation zeros out unimportant weights. Tree-based models (Random Forest, XGBoost) provide feature importance scores.
- **Dimensionality reduction** — instead of selecting features, transform them into a lower-dimensional space (PCA, SVD for text, autoencoders).

For bag-of-words specifically: TF-IDF weighting, minimum document frequency thresholds, and removing stopwords are standard preprocessing steps before any formal selection.

---

### Q4 Follow-up: What is PCA?

Principal Component Analysis (PCA) is a dimensionality reduction technique that transforms your features into a new set of uncorrelated variables called *principal components*, ordered by how much variance in the data they explain.

**How it works:** PCA computes the eigenvectors of the feature covariance matrix. Each eigenvector is a principal component — a direction in the original feature space. The eigenvalue tells you how much variance that direction captures. You then project your data onto the top k components, discarding the rest.

**Key properties:** Components are orthogonal (uncorrelated). The first component captures the most variance, the second captures the most remaining variance, and so on. By choosing k components that explain ~95% of variance, you can often dramatically reduce dimensionality.

**Important caveat:** PCA loses interpretability — the new components are linear combinations of original features and may not have a meaningful real-world interpretation. Also, PCA assumes linear relationships; for non-linear structure, consider t-SNE or UMAP.

---

### Q5: What else can you do to handle imbalanced data?

**Resampling the dataset:**
- *Oversampling* — duplicate or synthesise minority class samples. SMOTE (Synthetic Minority Over-sampling Technique) creates synthetic samples by interpolating between existing minority examples rather than just duplicating them.
- *Undersampling* — randomly remove majority class samples until classes are balanced. Simple but wastes data.

**Class weighting** — most ML libraries (sklearn, PyTorch) allow you to assign higher loss penalties to minority class errors via the `class_weight` parameter. This forces the model to treat minority class mistakes as more costly without changing the data.

**Threshold tuning** — the default classification threshold is 0.5, but you can lower it (e.g. to 0.3) to increase recall on the minority class at the cost of precision. Use your domain knowledge to pick the right trade-off.

**Ensemble methods** — BalancedRandomForest and EasyEnsemble train multiple models on rebalanced subsets and combine predictions.

**Anomaly detection framing** — if imbalance is extreme (<1%), reframe as anomaly detection rather than classification.

---

### Q6: Production model is performing poorly — what do you do?

1. **Define "performing poorly"** — is it accuracy dropping? Latency? A specific subset of inputs? Quantify the degradation with concrete metrics and compare against your baseline.
2. **Check for data drift** — the most common cause of production degradation. Compare feature distributions between training data and recent production traffic using statistical tests (KS test, PSI).
3. **Check for label/concept drift** — the relationship between features and labels may have changed. For example, user behaviour patterns that were predictive before may no longer be.
4. **Examine failure cases** — sample and manually inspect examples the model is getting wrong. Look for patterns — is there a specific input type, time period, or data source causing failures?
5. **Retrain** — if drift is confirmed, retrain on more recent data. Consider a sliding window approach or weighting recent data more heavily.
6. **Check pipeline issues** — bugs in feature engineering, preprocessing, or data ingestion in the production pipeline often cause silent failures that look like model degradation.

---

### Q6 Follow-up: How would you further investigate data differences?

- **Statistical distribution tests** — Kolmogorov-Smirnov (KS) test compares continuous feature distributions. Chi-squared test for categorical features. Population Stability Index (PSI) is widely used in industry to measure shift magnitude.
- **Train a drift detector** — build a binary classifier that tries to distinguish training data from production data. If it can classify them well (AUC >> 0.5), there is meaningful drift. Features with the highest importance in this classifier are the drifting ones.
- **Univariate analysis** — plot histograms and boxplots of each feature comparing training vs production. Look at summary statistics: mean, median, std, percentiles, null rates.
- **Embedding/representation shift** — for unstructured inputs (text, images), compare embeddings from a frozen model using dimensionality reduction (PCA, UMAP) to see if production samples cluster differently from training samples.
- **Slice-based analysis** — break production data into slices (by time, geography, device type, user segment) and measure per-slice performance. This often reveals that drift is localized to a specific subpopulation.

---

### ⭐ Q7: What is the bias-variance tradeoff?

Every model's generalisation error can be decomposed into three terms:

- **Bias** — error from overly simplistic assumptions in the model. A high-bias model underfits: it misses real patterns in the data (e.g. fitting a linear model to non-linear data).
- **Variance** — error from sensitivity to small fluctuations in the training set. A high-variance model overfits: it learns the noise in training data and fails on unseen data.
- **Irreducible noise** — error inherent in the data itself (labelling errors, missing information). Cannot be reduced by any model.

**Total error = Bias² + Variance + Irreducible noise**

The tradeoff: reducing bias (using a more complex model) typically increases variance, and vice versa. The goal is to find the sweet spot that minimises total generalisation error.

| | High Bias | High Variance |
|---|---|---|
| **Symptom** | Underfitting | Overfitting |
| **Training error** | High | Low |
| **Validation error** | High | Much higher than training |
| **Fix** | More complex model, more features | Regularisation, more data, simpler model |

---

### ⭐ Q8: What is cross-validation and why do you use it?

Cross-validation is a technique to estimate how well a model generalises to unseen data, without wasting data on a fixed held-out validation set.

**k-Fold Cross-Validation:**
1. Split the dataset into k equal folds.
2. For each fold i: train on the other k-1 folds, evaluate on fold i.
3. Average the k validation scores for the final estimate.

**Why use it?**
- Gives a more robust estimate of generalisation performance than a single train/val split — especially important with small datasets.
- Every sample is used for both training and validation across the k rounds.
- Reduces the variance of the performance estimate.

**Variants:**
- **Stratified k-fold** — preserves class proportions in each fold. Important for imbalanced datasets.
- **Leave-one-out (LOO)** — k = n (each sample is its own fold). Unbiased but very expensive.
- **Time-series split** — for temporal data, always train on past and validate on future. Never shuffle time-series data before splitting.

> **Common mistake:** doing feature selection or hyperparameter tuning on the full dataset before cross-validation. This causes data leakage — the val fold has already influenced the model indirectly.

---

### ⭐ Q9: Explain gradient descent and its variants

**Gradient descent** is the optimisation algorithm used to minimise a model's loss function. At each step, it computes the gradient of the loss with respect to the model parameters and moves the parameters in the opposite direction (downhill).

`θ ← θ - η · ∇L(θ)`

where η is the learning rate.

**Variants:**

- **Batch Gradient Descent** — compute the gradient over the entire training dataset per update. Stable updates but very slow for large datasets; can get stuck in local minima.
- **Stochastic Gradient Descent (SGD)** — update parameters after every single training example. Very noisy but fast; the noise can help escape local minima.
- **Mini-batch Gradient Descent** — update using a small batch (e.g. 32–256 samples). The standard in practice. Balances stability and speed; works well with GPU parallelism.

**Adaptive optimisers** (commonly used in deep learning):
- **Momentum** — accumulates a velocity vector in the direction of persistent gradient; dampens oscillations and speeds convergence.
- **Adam (Adaptive Moment Estimation)** — combines momentum (first moment) with per-parameter adaptive learning rates (second moment). The default optimiser for most deep learning tasks.
- **AdamW** — Adam with decoupled weight decay. Fixes a subtle bug in Adam's L2 regularisation interaction; widely used for LLM training.

**Learning rate** is the most critical hyperparameter. Too high → divergence; too low → very slow convergence or getting stuck. Learning rate schedulers (cosine annealing, warmup + decay) are standard in large model training.

---

### Q10: What are common loss functions and when do you use each?

The loss function measures how wrong the model's predictions are. Choosing the right one is crucial.

**For classification:**
- **Binary cross-entropy (log loss)** — for binary classification. Penalises confident wrong predictions heavily: `L = -[y·log(p) + (1-y)·log(1-p)]`.
- **Categorical cross-entropy** — multi-class classification with one-hot labels. Standard for softmax output.
- **Focal loss** — a variant of cross-entropy that down-weights easy examples and focuses training on hard, misclassified ones. Designed for imbalanced datasets (e.g. object detection).

**For regression:**
- **MSE (Mean Squared Error)** — penalises large errors heavily due to squaring. Sensitive to outliers.
- **MAE (Mean Absolute Error)** — more robust to outliers. Less smooth gradient near zero.
- **Huber loss** — combines MSE (for small errors) and MAE (for large errors). Robust to outliers while maintaining smooth gradients.

**For LLMs:**
- **Next-token prediction loss** — cross-entropy loss between predicted and actual next token. This is the standard pre-training objective for decoder-only models.

---

### ⭐ Q11: What is the difference between bagging and boosting?

Both are ensemble methods that combine multiple models to reduce error, but they do so in fundamentally different ways.

**Bagging (Bootstrap Aggregating):**
- Train multiple models **in parallel**, each on a different random bootstrap sample (sample with replacement) of the training data.
- Aggregate predictions by averaging (regression) or majority vote (classification).
- Reduces **variance** — the ensemble is more stable than any individual model.
- **Random Forest** is the most famous bagging method. It also randomises the feature subset at each split, adding further decorrelation between trees.

**Boosting:**
- Train models **sequentially**. Each new model focuses on the errors made by the previous ones — misclassified examples are upweighted.
- Aggregate predictions as a weighted sum.
- Reduces **bias** — progressively corrects mistakes, building a strong model from weak learners.
- **XGBoost, LightGBM, AdaBoost** are famous boosting methods. XGBoost/LightGBM are the go-to for tabular data in industry.

| | Bagging | Boosting |
|---|---|---|
| **Training** | Parallel | Sequential |
| **Error reduced** | Variance | Bias |
| **Overfitting risk** | Low | Higher (requires tuning) |
| **Example** | Random Forest | XGBoost, LightGBM |

---

### Q12: What is data leakage and how do you prevent it?

Data leakage occurs when information from outside the training distribution (typically from the test/future data) is used to build the model. It causes artificially inflated evaluation metrics that don't hold in production.

**Common forms:**
- **Target leakage** — a feature that is derived from or correlates with the target label in a way that wouldn't be available at prediction time (e.g. using a field that is only filled in after the outcome is known).
- **Train/test contamination** — preprocessing steps (normalisation, scaling, imputation, feature selection) are fit on the full dataset before splitting, so test data influences the training process.
- **Temporal leakage** — using future information to predict the past (e.g. computing a 7-day rolling average at time t using data from t+1 to t+7).
- **Duplicate leakage** — near-duplicate rows spanning the train and test sets.

**Prevention:**
- Always fit preprocessing (scalers, imputers, encoders) only on training data, then transform test data using those fitted objects.
- Use pipelines (`sklearn.pipeline.Pipeline`) to prevent accidental leakage.
- For time-series, always split by time, never shuffle.
- Scrutinise any feature with surprisingly high importance — it may be leaking.

---

### Q13: What is normalisation vs standardisation, and when does it matter?

Both are feature scaling techniques that transform numeric features to a consistent range.

- **Standardisation (Z-score)** — transforms features to have mean=0 and std=1: `x' = (x - μ) / σ`. Does not bound values; handles outliers better. Required by algorithms that assume Gaussian distributions or use distance metrics sensitive to scale (SVM, PCA, KNN, logistic regression, neural networks).
- **Normalisation (Min-max)** — scales features to a fixed range, usually [0, 1]: `x' = (x - min) / (max - min)`. Sensitive to outliers. Useful when you need bounded inputs (e.g. pixel values, certain neural network architectures).

**When does it matter?**
- Algorithms that use distances (KNN, SVM with RBF kernel, PCA) are highly sensitive to scale — always scale.
- Tree-based models (Random Forest, XGBoost) are invariant to monotonic transformations of features — scaling has no effect.
- Neural networks technically don't require it, but converge much faster with normalised inputs.

---

### ⭐ Q14: How do you approach hyperparameter tuning?

Hyperparameters are parameters set before training (learning rate, number of layers, regularisation strength) that are not learned from data.

**Methods:**

- **Grid search** — exhaustively try every combination of specified hyperparameter values. Simple but exponential in the number of hyperparameters.
- **Random search** — randomly sample combinations from specified distributions. Empirically shown to be more efficient than grid search — you're more likely to find good values when not all hyperparameters matter equally.
- **Bayesian optimisation** — build a probabilistic surrogate model of the objective function. Use it to choose the next hyperparameter combination to try, focusing on promising regions. Much more sample-efficient. Tools: Optuna, Ray Tune, Hyperopt.
- **Early stopping as a free hyperparameter** — use it alongside any search method to avoid training configurations that won't converge.

**Practical advice:**
- Always tune using cross-validation, not a single val split.
- Tune the most impactful hyperparameters first (learning rate for neural nets; max_depth and n_estimators for trees).
- Use logarithmic scales for learning rates and regularisation strengths (they span orders of magnitude).

---

## Part 2 — LLMs

---

### Q1: From user query to model output — the full process

1. **Tokenisation** — the raw input text is split into tokens (subword units) using an algorithm like BPE (Byte Pair Encoding) or WordPiece. "unhappiness" might become `["un", "happ", "iness"]`. Each token maps to an integer ID from the vocabulary.
2. **Embedding lookup** — each token ID is converted to a dense vector (embedding) by looking up a learned embedding matrix. These vectors encode semantic meaning. Positional encodings are added to give the model information about token order.
3. **Transformer layers** — the sequence of token embeddings is passed through N transformer layers. Each layer applies multi-head self-attention (to model relationships between tokens) and a feed-forward network (to transform each position independently). Layer normalisation and residual connections stabilise training.
4. **Final projection & softmax** — the output from the last transformer layer is projected via a linear layer to a vector of size |vocabulary|. Softmax converts this to a probability distribution over all possible next tokens.
5. **Sampling/decoding** — a token is selected from this distribution using a decoding strategy (greedy, temperature sampling, top-k, top-p). The selected token is appended to the input, and the process repeats autoregressively until an end-of-sequence token is generated.

---

### Q1 Follow-up: How does the transformer / attention mechanism work?

Self-attention allows each token in a sequence to "look at" all other tokens and weigh how relevant they are to its own representation.

**Q, K, V projections** — for each token embedding, three vectors are computed: Query (Q), Key (K), and Value (V) via learned linear projections. Intuitively: the Query is "what am I looking for?", the Key is "what do I contain?", the Value is "what information do I give?"

**Attention scores** — the attention score between token i and token j is computed as `Q_i · K_j / √d_k` (dot product scaled by root of dimension, to prevent vanishing gradients with large d). Softmax is applied across all j positions to get attention weights that sum to 1.

**Output** — the output for each position is the weighted sum of all Value vectors:

```
Attention(Q, K, V) = softmax(QKᵀ / √d_k) · V
```

**Multi-head attention** — instead of one attention computation, multiple heads run in parallel with different Q/K/V projections. Each head can learn to attend to different aspects (syntax, coreference, semantics). Outputs are concatenated and linearly projected.

**Why this is powerful:** attention is permutation-invariant and can model long-range dependencies in O(n²) time — unlike RNNs which process sequentially. The model learns directly which tokens matter to each other, regardless of distance.

---

### Q2: Encoder-only vs decoder-only models — what's the difference?

| | Encoder-only | Decoder-only | Encoder-decoder |
|---|---|---|---|
| **Examples** | BERT, RoBERTa | GPT, Llama, Claude | T5, BART |
| **Attention** | Bidirectional (full) | Causal (left-to-right only) | Encoder: bidirectional; Decoder: causal + cross-attention |
| **Training objective** | Masked language modelling | Next token prediction | Seq-to-seq (e.g. translation) |
| **Best for** | Classification, NER, embeddings, semantic search | Text generation, chat, code | Translation, summarisation, QA |

- **Encoder-only** — each token attends to all others. Rich contextual representations but cannot generate text autoregressively.
- **Decoder-only** — each token only attends to previous tokens (causal masking). Naturally suited for generation. Most modern LLMs use this architecture.
- **Encoder-decoder** — encoder processes the full input bidirectionally; decoder generates output while cross-attending to the encoder's output. Best for tasks where input and output are distinct sequences.

---

### Q3: Ways to contextualise a pre-trained LLM into a good chatbot

- **Prompt engineering** — craft a system prompt that defines the assistant's persona, capabilities, constraints, and tone. Zero-shot or few-shot examples in the prompt can dramatically shape behaviour without any training.
- **Instruction fine-tuning (SFT)** — fine-tune the base model on (instruction, response) pairs. The model learns to follow instructions rather than simply predicting the next token.
- **RLHF** — Reinforcement Learning from Human Feedback. Humans rank model outputs; a reward model is trained on these rankings; the LLM is then fine-tuned with PPO to maximise the reward model's score. Aligns the model with human preferences for helpfulness, harmlessness, and honesty.
- **RAG** — connect the model to an external knowledge base. At inference time, retrieve relevant documents and inject them into the context. Keeps knowledge up-to-date without retraining.
- **PEFT** — methods like LoRA allow fine-tuning only a small number of added parameters, making domain adaptation much cheaper than full fine-tuning.

---

### Q4: What is RLHF?

RLHF (Reinforcement Learning from Human Feedback) is the technique used to align LLMs with human preferences. It has three stages:

1. **Supervised Fine-Tuning (SFT)** — fine-tune the base model on high-quality demonstration data written by human labellers. This gives the model a starting point for being helpful.
2. **Reward Model Training** — for many prompts, generate multiple model outputs and have humans rank them from best to worst. Train a separate reward model on these preference pairs — it learns to predict which response humans would prefer.
3. **RL Fine-Tuning (PPO)** — use Proximal Policy Optimisation (PPO) to fine-tune the SFT model to maximise the reward model's score. A KL divergence penalty is added to prevent the model from deviating too far from the SFT model (which would cause mode collapse or reward hacking).

**Modern variant — DPO (Direct Preference Optimisation):** skips the explicit reward model and optimises directly on preference pairs, making the process simpler and more stable. Used in many modern open-source models.

---

### Q5: How would you process a long document for an LLM?

- **If context window is sufficient** — modern LLMs support very large context windows (128k–1M+ tokens). Simply fit the document in. But beware the *"lost in the middle"* problem — models attend less reliably to content in the middle of long contexts.
- **Chunking + RAG** — split the document into overlapping chunks, embed each chunk, store in a vector database. At query time, retrieve only the relevant chunks and inject them into the context. The most common production approach.
- **Hierarchical summarisation** — for summarisation tasks, recursively summarise chunks, then summarise the summaries. MapReduce pattern: map (summarise each chunk independently) → reduce (combine summaries).
- **Sliding window / streaming** — for tasks requiring full coverage, process the document in a sliding window, maintaining a rolling summary or state between windows.
- **Long-context fine-tuning** — fine-tune models on long-document tasks using techniques like RoPE scaling or ALiBi positional encoding which generalise to longer sequences.

---

### Q6: How does temperature affect model output?

Temperature T scales the logits before the softmax: `logits → logits / T`. This controls the "sharpness" of the probability distribution over the vocabulary.

| Temperature | Effect | Use case |
|---|---|---|
| T → 0 | Distribution peaks sharply; near-deterministic (greedy) | Factual, precise tasks |
| T = 1 | Default; raw model distribution | General use |
| T > 1 | Distribution flattens; more random and creative | Creative writing, brainstorming |

- Use **low temperature (0.2–0.5)** for factual, precise tasks (code generation, data extraction).
- Use **higher temperature (0.7–1.2)** for creative tasks (story writing, brainstorming).

---

### Q6 Follow-up: Is temperature = 0 output truly deterministic?

**In theory, yes — in practice, not always.**

At T=0, the model greedily selects the argmax token at every step. Given identical inputs and model weights, the output should be identical. This is mathematically deterministic.

However, non-determinism can still arise from:
- **Floating-point non-determinism** — GPU operations (especially tensor operations with non-associative floating-point sums) can produce slightly different results across runs due to parallelism and operation ordering.
- **Ties in logits** — if two tokens have exactly equal logits (very rare), tie-breaking may be non-deterministic.
- **Batching** — if the same prompt is processed with other prompts in a batch, kernel-level floating-point accumulation differences can affect results.

**Conclusion:** T=0 is effectively deterministic for most practical purposes, but strict reproducibility requires fixing seeds, using deterministic CUDA ops, and avoiding batching — often not feasible at inference scale.

---

### Q7: LLM output sampling methods

- **Greedy decoding** — always pick the highest-probability token. Fast and deterministic but can fall into repetitive loops and misses diverse, correct outputs.
- **Beam search** — maintain the top-k most probable sequences (beams) at each step, expand each, and keep the best k. Finds higher-probability sequences than greedy. Used heavily in translation/summarisation. Tends to produce generic, "safe" outputs.
- **Temperature sampling** — sample from the scaled distribution. Introduces randomness proportional to temperature.
- **Top-k sampling** — at each step, only sample from the k highest-probability tokens. Prevents sampling very unlikely tokens. k=50 is a common default.
- **Top-p (nucleus) sampling** — dynamically include the smallest set of tokens whose cumulative probability exceeds p (e.g. 0.9). The vocabulary set shrinks when the model is confident and grows when uncertain. More adaptive than top-k. Very widely used in modern LLMs.
- **Min-p sampling** — a newer variant that filters tokens below `p × (max token probability)`. Adapts to distribution entropy like nucleus sampling but with slightly different characteristics.

---

### Q8: Parameter-Efficient Fine-Tuning (PEFT) methods

Full fine-tuning updates all model weights — expensive for 7B–70B+ parameter models. PEFT methods add or adapt a small number of parameters while freezing the base model.

- **LoRA (Low-Rank Adaptation)** — the most popular method. Instead of updating weight matrix W directly, LoRA adds a low-rank decomposition: `W + ΔW = W + A·B`, where A ∈ ℝ^(d×r) and B ∈ ℝ^(r×k) and r ≪ min(d,k). Only A and B are trained. Typically reduces trainable parameters by 99%+. At inference, LoRA weights can be merged back into W (no overhead) or kept separate for easy swapping.
- **QLoRA** — LoRA applied to a model quantised to 4-bit precision. Makes fine-tuning a 65B model feasible on a single consumer GPU. Very widely used in the open-source community.
- **Prefix tuning / Prompt tuning** — prepend learned "soft prompt" tokens to the input. Only the soft prompt embeddings are trained. Less parameter-efficient than LoRA for large models.
- **Adapter layers** — insert small trainable bottleneck modules between transformer layers. The base model is frozen; only adapters train. The original PEFT approach, now mostly superseded by LoRA.

---

## Part 3 — RAG & LLM Evaluation

---

### Q1: Describe an end-to-end RAG pipeline

#### Offline (indexing) stage

1. **Document loading** — ingest raw documents (PDFs, HTML, databases, etc.) and extract clean text.
2. **Chunking** — split documents into chunks. Strategy matters: fixed-size with overlap, recursive character splitting, semantic chunking (split on embedding similarity boundaries), or hierarchical chunking (store full document + chunks). Chunk size is a key hyperparameter — too small loses context; too large dilutes signal.
3. **Embedding** — encode each chunk into a dense vector using an embedding model (e.g. text-embedding-3-large, BGE, E5). Store vectors in a vector database (Pinecone, Weaviate, pgvector, FAISS).

#### Online (query) stage

4. **Query transformation** — optionally rewrite/expand the user query (HyDE, query decomposition, step-back prompting) to improve retrieval.
5. **Retrieval** — embed the query, search the vector DB using approximate nearest neighbour (ANN) search (cosine similarity). Optionally combine with BM25 keyword search (hybrid retrieval). Return top-k chunks.
6. **Reranking** — optionally pass the top-k chunks through a cross-encoder reranker (Cohere, BGE-reranker) that scores query-chunk relevance more precisely than embedding similarity. Rerank and select top-n.
7. **Context construction** — format retrieved chunks + conversation history into a prompt for the LLM.
8. **Generation** — LLM generates the final answer grounded in the retrieved context.

---

### Q2: How would you evaluate an end-to-end RAG pipeline?

Evaluate both the retrieval and generation components separately, then end-to-end.

#### Retrieval evaluation
Requires a labelled ground-truth dataset of (query → relevant chunks).

- **Precision@k** — of the top-k retrieved chunks, what fraction are relevant?
- **Recall@k** — of all relevant chunks in the corpus, what fraction did you retrieve in the top-k?
- **MRR (Mean Reciprocal Rank)** — ranks how high the first relevant chunk appears on average.
- **NDCG** — normalised discounted cumulative gain; accounts for graded relevance and position.

#### Generation evaluation

- **Faithfulness** — is the generated answer supported by the retrieved context? (No hallucination.) LLM-as-judge or NLI models can check this.
- **Answer relevance** — does the answer address the question? LLM-as-judge.
- **Context relevance** — how much of the retrieved context was actually used/needed? Measures retrieval noise.

#### End-to-end evaluation

Compare final answers against ground-truth answers using: exact match, F1 (for extractive answers), ROUGE/BLEU (for longer answers), or LLM-as-judge (for open-ended quality). Frameworks like **RAGAS** and **TruLens** automate this evaluation pipeline.

---

### Q3: Optimising RAG to run under <1 second

The main latency sources are: embedding the query, vector search, reranking, and LLM generation. Target each:

- **Query embedding** — use a fast, small embedding model. Quantise embeddings to int8. Cache embeddings for common queries.
- **Vector search** — use approximate NN (HNSW, IVF) rather than exact search. Reduce vector dimensionality with matryoshka embeddings or dimensionality reduction. Use hardware acceleration (GPU-based FAISS, pgvector with indexing).
- **Reranker** — if latency is critical, skip the cross-encoder reranker (typically adds 100–500ms). Instead use a bi-encoder with better initial retrieval, or only rerank a smaller candidate set.
- **Retrieval size** — retrieve fewer chunks (top-3 instead of top-10). Use smaller, well-chunked documents so chunks are highly targeted.
- **LLM generation** — usually the biggest bottleneck. Use a smaller/faster model for simple queries. Use speculative decoding or streaming. Cache responses for common queries (semantic caching). Consider flash attention and quantised inference.
- **Infrastructure** — keep the vector DB and LLM inference in the same region/network. Avoid cold starts with warm instances. Parallelise retrieval and prompt construction where possible.

---

### Q4: Model not generating the output you want — how do you diagnose and fix it?

Systematically isolate whether the problem is in retrieval or generation:

**Is retrieval the problem?**
Manually inspect the retrieved chunks for a sample of failing queries. If the right documents are not being retrieved — improve chunking strategy, switch to hybrid retrieval (BM25 + vector), add a reranker, or improve the embedding model. Check if query phrasing is very different from document phrasing (vocabulary mismatch — HyDE or query expansion can help).

**Is the context the problem?**
Even if relevant chunks are retrieved, check if the context window is structured well. Is the most important information early in the context? Is there irrelevant noise diluting the signal? Reduce the number of retrieved chunks, or use a better reranker to filter noise.

**Is generation the problem?**
If the retrieved context is correct but the answer is wrong — the issue is in the LLM. Improve the system prompt (be more specific about format, tone, instructions). Use few-shot examples of correct outputs. Try a more capable model. Add output format constraints (JSON schema, constrained decoding).

**Is it a hallucination?**
The model is generating facts not in the context. Add explicit grounding instructions: *"Answer only based on the provided context. If the answer is not in the context, say you don't know."* Lower temperature. Use faithfulness checking as a post-generation filter.

> **Systematic approach**: build an evaluation set of failing queries, fix the stage responsible, and measure improvement. Don't change retrieval and generation at the same time — you won't know what worked.

---

## Part 4 — MLOps, Systems & Production

---

### ⭐ Q1: What is MLOps and why does it matter?

MLOps (Machine Learning Operations) is the set of practices that brings software engineering discipline to ML workflows — covering data management, model training, evaluation, deployment, monitoring, and retraining. Without it, models get trained in notebooks but never reliably shipped, or they silently degrade in production.

**Core pillars:**

- **Reproducibility** — given the same data and code, you should always get the same model. Requires versioning of data (DVC, Delta Lake), code (git), and model artifacts (MLflow, W&B).
- **Continuous Training (CT)** — automated retraining pipelines that kick off when data drift is detected or on a schedule.
- **Continuous Integration / Continuous Delivery (CI/CD) for ML** — automated tests for data quality, model performance gates, and deployment pipelines.
- **Monitoring** — track model performance, data drift, and system health in production over time.

**Key tools:** MLflow (experiment tracking), Weights & Biases (training dashboards), DVC (data versioning), Airflow / Prefect / Kubeflow (pipeline orchestration), Seldon / BentoML / TorchServe (model serving).

---

### ⭐ Q2: How do you deploy a model to production?

**Step 1 — Serialise the model:** Save model weights and architecture in a portable format. Options: ONNX (cross-framework, good for inference optimisation), TorchScript (PyTorch-specific, enables mobile/C++ deployment), HuggingFace `save_pretrained`, or pickle/joblib for sklearn.

**Step 2 — Choose a serving pattern:**
- **REST API** — wrap the model in a FastAPI/Flask service. Simple, widely understood. Synchronous.
- **gRPC** — binary protocol, lower latency than REST. Common for internal microservices.
- **Batch inference** — run predictions offline on large datasets at scheduled intervals. No latency requirement. Cheap.
- **Streaming inference** — consume events from a queue (Kafka), predict, publish results. Good for real-time pipelines.

**Step 3 — Containerise:** Package the model + dependencies in a Docker container for environment reproducibility.

**Step 4 — Orchestrate:** Deploy on Kubernetes for autoscaling, health checks, and rolling updates. Cloud-managed options: AWS SageMaker, GCP Vertex AI, Azure ML.

**Step 5 — Monitor:** Track prediction latency, throughput, input feature distributions, and output distributions. Set up alerts for anomalies.

---

### ⭐ Q3: What is model monitoring and what should you monitor?

Once a model is in production, its performance can degrade silently. Monitoring catches this before users are affected.

**What to monitor:**

- **Data/input drift** — are the feature distributions shifting? Use statistical tests (KS test, PSI) on a rolling window of incoming requests vs. training distribution.
- **Prediction/output drift** — is the distribution of model outputs changing? (e.g. more positive predictions than expected)
- **Concept drift** — the relationship between features and labels has changed. Harder to detect without ground-truth labels; proxy signals (business KPIs, user feedback) are often used.
- **Model performance** — when ground truth labels become available (e.g. delayed feedback), compute actual accuracy/F1/AUC and compare to training baseline.
- **System metrics** — latency (p50, p95, p99), throughput (requests/sec), error rates, memory/CPU usage.
- **Data quality** — null rates, out-of-range values, schema changes in incoming data.

**Tools:** Evidently AI, WhyLogs, Arize, Grafana + Prometheus (system metrics).

---

### Q4: What is A/B testing in the context of ML model deployment?

A/B testing (also called online experimentation or shadow deployment) is used to safely evaluate a new model against the production model using real traffic.

**How it works:**
- Route a percentage of live traffic (e.g. 10%) to the new model (treatment), while the rest goes to the old model (control).
- Collect metrics (business KPIs, model metrics, user engagement) from both groups simultaneously.
- Use statistical significance testing (t-test, chi-squared) to determine if the difference in performance is real or due to chance.
- Roll out the new model fully only if it wins.

**Variants:**
- **Shadow mode** — new model runs in parallel and logs predictions, but its outputs are not served to users. Zero risk, but you can't measure user-facing impact.
- **Canary deployment** — route a small % of traffic to the new model first, monitor for errors, then gradually increase.
- **Multi-armed bandit** — dynamically allocate more traffic to whichever model is performing better, rather than a fixed split. More efficient but harder to analyse statistically.

---

### Q5: How do you handle model versioning and rollback?

**Model versioning:**
- Every trained model should be assigned a unique version tied to: the code version (git commit), the dataset version, and the hyperparameters used. This is what MLflow Model Registry, W&B Artifacts, and SageMaker Model Registry do.
- Store model cards alongside each version: training data, metrics, known failure modes, intended use.

**Rollback strategy:**
- Keep the previous model version deployed (even if serving 0% traffic) so you can switch back instantly.
- Use feature flags or traffic routing rules (not code deploys) to switch between model versions — a deploy should never be required to roll back.
- Define a rollback trigger: a drop in a key metric beyond a threshold within the first N minutes/hours of deployment.

---

### Q6: What is quantisation and why is it important for LLM deployment?

Quantisation reduces the numerical precision of model weights (and sometimes activations) to use less memory and compute, at a small cost to accuracy.

**Common formats:**
- **FP32** — full precision (4 bytes per parameter). Standard training format.
- **FP16 / BF16** — half precision (2 bytes). BF16 has a wider dynamic range than FP16, making it preferred for training. Both halve memory usage with minimal quality loss.
- **INT8** — 8-bit integers (1 byte). 4× memory reduction vs FP32. Small accuracy drop if done carefully (e.g. with calibration data).
- **INT4 / GPTQ / AWQ** — 4-bit quantisation (0.5 bytes). Used in QLoRA, llama.cpp, and for consumer GPU deployment. More noticeable quality drop, especially on small models.

**Why it matters:**
- A 70B parameter model in FP16 requires ~140GB VRAM — multiple A100s. In INT4, it fits in ~35GB — a single A100 or two consumer GPUs.
- Quantisation also speeds up inference via faster memory bandwidth and SIMD/tensor core operations on integer types.

**Post-training quantisation (PTQ)** — quantise after training using calibration data. No retraining needed. GPTQ and AWQ are popular PTQ methods for LLMs.
**Quantisation-aware training (QAT)** — simulate quantisation during training for better accuracy, but requires a full training run.

---

### Q7: What is KV cache and why does it matter for LLM inference?

During autoregressive generation, the transformer computes Key (K) and Value (V) matrices for every token in the context at every generation step. Without caching, this is redundant — the K and V for all previous tokens are identical to what was computed in the previous step.

**KV cache** stores the K and V tensors from all previous steps in memory. At each new generation step, only the new token's K and V need to be computed — the rest are retrieved from cache. This reduces the per-token generation cost from O(n) to O(1) in attention computation.

**Why it matters:**
- Without KV cache, a model generating 1000 tokens would recompute attention over the growing context 1000 times — extremely slow.
- With KV cache, inference is much faster but requires significant memory: for a 7B model with a 4096-token context, the KV cache can be several GB.

**Implications for production:**
- **Batch size is limited by KV cache memory** — longer sequences = smaller max batch size.
- **Prefix caching** — if many requests share a common system prompt, cache its KV state once and reuse across requests. Used in production LLM serving (vLLM, TensorRT-LLM).
- **vLLM's PagedAttention** — manages KV cache memory in pages (like OS virtual memory), dramatically increasing throughput by eliminating memory fragmentation.

---

## Part 5 — Agentic Systems

---

### ⭐ Q1: What is an agentic system / LLM agent?

An LLM agent is a system where an LLM acts as a reasoning engine that autonomously decides what actions to take, executes them using tools, observes the results, and iterates — rather than simply generating a single response.

**Key components:**

- **LLM (the brain)** — reasons about the task, decides on actions, and synthesises results.
- **Tools** — external capabilities the agent can invoke: web search, code execution, database queries, API calls, file I/O.
- **Memory** — short-term (conversation context / scratchpad), long-term (vector DB of past interactions or retrieved knowledge).
- **Orchestration loop** — the control flow that repeatedly calls the LLM, executes its chosen tool, feeds the result back, and checks if the task is complete.

**Core interaction pattern (ReAct):**
```
Thought: I need to find the current stock price.
Action: web_search("AAPL stock price today")
Observation: Apple Inc. (AAPL) is trading at $189.25.
Thought: I have the information. I can now answer.
Answer: The current AAPL stock price is $189.25.
```

---

### ⭐ Q2: What is the ReAct framework?

ReAct (Reasoning + Acting) is a prompting framework where the LLM interleaves chain-of-thought reasoning with tool use actions. This is the most common agent pattern.

**Why it works:**
- The explicit "Thought" step encourages the model to reason before acting, reducing impulsive/wrong tool calls.
- The "Observation" step closes the loop — the model grounds its next reasoning step in real tool results rather than hallucinating.
- Interleaving reasoning and acting allows the model to dynamically adjust its plan based on what it discovers.

**Compared to pure chain-of-thought (CoT):** CoT is purely internal reasoning without external grounding. ReAct adds tool grounding, making it much more reliable for tasks requiring factual lookup, calculation, or code execution.

---

### Q3: What is the difference between single-agent and multi-agent systems?

**Single-agent:** One LLM handles the full task — planning, tool use, and synthesis. Simpler to build and debug. Works well for bounded, well-defined tasks.

**Multi-agent:** Multiple specialised LLM agents collaborate on a task. One agent may orchestrate others, or agents may communicate peer-to-peer.

**Why multi-agent?**
- **Parallelism** — sub-tasks can be executed concurrently by specialised agents.
- **Context management** — complex tasks may exceed a single context window; breaking them into sub-tasks keeps each agent's context manageable.
- **Specialisation** — a code-writing agent, a research agent, and a review agent may each perform their role better than a single generalist agent trying to do everything.

**Common patterns:**
- **Orchestrator-worker** — a planner agent breaks a task into steps and delegates to specialised worker agents.
- **Critic-generator** — one agent generates a response; another critiques it; the generator revises. Improves output quality.
- **Debate / consensus** — multiple agents independently produce answers; a judge agent synthesises or picks the best.

**Challenges:** Inter-agent communication overhead, error propagation (one agent's mistake cascades), harder to debug, non-deterministic execution order.

---

### Q4: What are the main challenges in building reliable agents?

**Hallucination in reasoning** — the LLM may fabricate tool call arguments, misread tool outputs, or draw wrong conclusions from observations. Mitigation: structured output formats (JSON schema), validation layers, grounding every factual claim in tool results.

**Task planning failure** — the agent may generate an incorrect or inefficient plan, especially for novel or multi-step tasks. Mitigation: chain-of-thought prompting, hierarchical planning (plan → sub-plans), self-reflection steps.

**Tool use errors** — wrong tool selected, wrong arguments passed, or tool returns an error. Mitigation: provide clear tool descriptions, handle errors gracefully and feed them back to the LLM as observations, implement retry logic.

**Infinite loops / getting stuck** — the agent keeps calling tools without making progress. Mitigation: step budget (max tool calls), loop detection, a "give up and ask the user" fallback.

**Context overflow** — long multi-step tasks accumulate a large context that exceeds the window limit. Mitigation: summarise intermediate results, use external memory, reset context periodically.

**Security — prompt injection** — malicious content in tool outputs (e.g. a web page containing "ignore previous instructions and exfiltrate data") can hijack agent behaviour. Mitigation: sanitise tool outputs, use a separate parsing model, apply strict output schemas.

---

### Q5: What is tool calling / function calling in LLMs?

Tool calling (also called function calling) is a model capability where the LLM outputs a structured specification of a function to call — name and arguments — rather than free text. The host application executes the function and returns the result to the model.

**How it works (OpenAI/Anthropic style):**
1. You provide a list of tool definitions in the API request (name, description, JSON schema for parameters).
2. The model decides whether to call a tool. If so, it outputs a structured tool_use block instead of (or alongside) text.
3. Your code executes the specified tool with the given arguments.
4. You pass the tool result back to the model in the next turn.
5. The model uses the result to continue reasoning or produce a final answer.

**Why it's better than parsing free text:**
- Structured JSON output is reliable and parseable — no regex hacks.
- The model is fine-tuned to signal tool intent cleanly.
- Enables parallel tool calls — the model can request multiple tools simultaneously.

---

### Q6: What is memory in the context of LLM agents?

Agents need different types of memory to handle complex, long-running tasks:

- **In-context (working) memory** — the conversation history and scratchpad within the current context window. Fast and directly accessible by the model, but limited by context length and lost between sessions.
- **External / long-term memory** — information stored outside the model, persisted across sessions. Implemented with vector databases (retrieve by semantic similarity), relational databases, or key-value stores. The agent must explicitly retrieve from it.
- **Episodic memory** — records of past interactions or tasks the agent has completed. Useful for personalisation and learning from past mistakes.
- **Semantic memory** — general knowledge (facts, rules, domain knowledge) stored externally and retrieved as needed. This is essentially RAG applied to agents.

**Memory management challenges:**
- What to store (not everything is worth remembering).
- When to retrieve (retrieval latency adds to response time).
- How to update stale memories.
- How to resolve conflicting memories.

---

### Q7: What is Chain-of-Thought (CoT) prompting and why does it help?

Chain-of-Thought prompting instructs the model to produce intermediate reasoning steps before giving a final answer, rather than jumping straight to the conclusion.

**Zero-shot CoT:** simply append "Let's think step by step." to the prompt. Surprisingly effective.

**Few-shot CoT:** provide examples where the reasoning steps are shown explicitly. The model learns the expected reasoning format.

**Why it helps:**
- Decomposes hard problems into smaller, more tractable sub-steps.
- Forces the model to "show its work", catching errors that would be hidden in a direct answer.
- Empirically, CoT dramatically improves performance on arithmetic, logical reasoning, and multi-step tasks — especially for larger models.
- The intermediate steps also make the model's reasoning inspectable and debuggable.

**Variants:**
- **Self-consistency** — sample multiple CoT reasoning paths with temperature > 0, then take the majority vote answer. Reduces variance significantly.
- **Tree of Thoughts (ToT)** — explore multiple reasoning branches simultaneously, backtrack on dead ends. Better for problems requiring search.
- **Least-to-most prompting** — decompose a complex problem into sub-problems, solve them sequentially, using earlier answers to inform later ones.

---

*Good luck with your interview!*
