import numpy as np


def relu(x):
    return np.maximum(0.0, x)


class NumpyAutoencoder:
    def __init__(self, layer_sizes=(8, 64, 32, 8, 32, 64, 8), seed=42):
        rng = np.random.default_rng(seed)
        self.sizes = list(layer_sizes)
        self.weights = []
        self.biases = []
        for fan_in, fan_out in zip(self.sizes[:-1], self.sizes[1:]):
            limit = np.sqrt(6.0 / (fan_in + fan_out))
            w = rng.uniform(-limit, limit, size=(fan_in, fan_out))
            b = np.zeros(fan_out)
            self.weights.append(w)
            self.biases.append(b)
        self.m_w = [np.zeros_like(w) for w in self.weights]
        self.v_w = [np.zeros_like(w) for w in self.weights]
        self.m_b = [np.zeros_like(b) for b in self.biases]
        self.v_b = [np.zeros_like(b) for b in self.biases]
        self.t = 0

    def forward(self, x):
        activations = [x]
        n_layers = len(self.weights)
        for i in range(n_layers - 1):
            z = activations[-1] @ self.weights[i] + self.biases[i]
            activations.append(relu(z))
        z = activations[-1] @ self.weights[-1] + self.biases[-1]
        activations.append(z)
        return activations

    def reconstruct(self, x):
        return self.forward(x)[-1]

    def _backward(self, activations):
        grads_w = [None] * len(self.weights)
        grads_b = [None] * len(self.biases)
        delta = 2.0 * (activations[-1] - activations[0]) / activations[0].shape[0]
        for i in range(len(self.weights) - 1, -1, -1):
            a_prev = activations[i]
            grads_w[i] = a_prev.T @ delta
            grads_b[i] = delta.sum(axis=0)
            if i > 0:
                delta = (delta @ self.weights[i].T) * (a_prev > 0)
        return grads_w, grads_b

    def _adam_step(self, grads_w, grads_b, lr=1e-3, beta1=0.9, beta2=0.999, eps=1e-8):
        self.t += 1
        for i in range(len(self.weights)):
            for params, grads, m_list, v_list in (
                (self.weights, grads_w, self.m_w, self.v_w),
                (self.biases, grads_b, self.m_b, self.v_b),
            ):
                g = grads[i]
                m_list[i] = beta1 * m_list[i] + (1 - beta1) * g
                v_list[i] = beta2 * v_list[i] + (1 - beta2) * (g * g)
                m_hat = m_list[i] / (1 - beta1 ** self.t)
                v_hat = v_list[i] / (1 - beta2 ** self.t)
                params[i] -= lr * m_hat / (np.sqrt(v_hat) + eps)

    def fit(self, x_train, x_val=None, epochs=100, batch_size=256, lr=1e-3, patience=10, verbose=True):
        best_val = np.inf
        best_state = None
        wait = 0
        rng = np.random.default_rng(7)
        n = x_train.shape[0]
        for epoch in range(1, epochs + 1):
            order = rng.permutation(n)
            losses = []
            for start in range(0, n, batch_size):
                idx = order[start:start + batch_size]
                batch = x_train[idx]
                acts = self.forward(batch)
                loss = float(np.mean((acts[-1] - batch) ** 2))
                losses.append(loss)
                gw, gb = self._backward(acts)
                self._adam_step(gw, gb, lr=lr)
            val_loss = None
            if x_val is not None and len(x_val) > 0:
                rec = self.reconstruct(x_val)
                val_loss = float(np.mean((rec - x_val) ** 2))
            if verbose and epoch % 5 == 0:
                msg = f"epoch {epoch:3d}  train_mse={np.mean(losses):.6f}"
                if val_loss is not None:
                    msg += f"  val_mse={val_loss:.6f}"
                print(msg)
            if val_loss is None:
                continue
            if val_loss < best_val - 1e-9:
                best_val = val_loss
                wait = 0
                best_state = ([w.copy() for w in self.weights], [b.copy() for b in self.biases])
            else:
                wait += 1
                if wait >= patience:
                    if verbose:
                        print(f"early stopping at epoch {epoch} (best val_mse={best_val:.6f})")
                    break
        if best_state is not None:
            self.weights, self.biases = best_state

    def reconstruction_error(self, x):
        rec = self.reconstruct(x)
        return np.mean((x - rec) ** 2, axis=1)

    def export(self):
        arrays = {}
        for i, (w, b) in enumerate(zip(self.weights, self.biases), start=1):
            arrays[f"W{i}"] = w
            arrays[f"b{i}"] = b
        return arrays

    @classmethod
    def load(cls, arrays, sizes):
        model = cls.__new__(cls)
        model.sizes = list(sizes)
        model.weights = [arrays[f"W{i}"] for i in range(1, len(sizes))]
        model.biases = [arrays[f"b{i}"] for i in range(1, len(sizes))]
        model.m_w = [np.zeros_like(w) for w in model.weights]
        model.v_w = [np.zeros_like(w) for w in model.weights]
        model.m_b = [np.zeros_like(b) for b in model.biases]
        model.v_b = [np.zeros_like(b) for b in model.biases]
        model.t = 0
        return model


def keras_to_numpy(keras_model):
    arrays = {}
    sizes = [int(keras_model.input_shape[-1])]
    for i, layer in enumerate(keras_model.layers, start=1):
        w, b = layer.get_weights()
        arrays[f"W{i}"] = np.asarray(w, dtype=np.float64)
        arrays[f"b{i}"] = np.asarray(b, dtype=np.float64).reshape(-1)
        sizes.append(int(arrays[f"W{i}"].shape[1]))
    return arrays, sizes
