import numpy as np


def build_keras_autoencoder(input_dim, latent_dim=8):
    try:
        from tensorflow import keras
        from tensorflow.keras import layers
    except ImportError:
        from tensorflow.keras import layers

    inp = keras.Input(shape=(input_dim,))
    x = layers.Dense(64, activation="relu")(inp)
    x = layers.Dense(32, activation="relu")(x)
    latent = layers.Dense(latent_dim, activation="relu", name="latent")(x)
    x = layers.Dense(32, activation="relu")(latent)
    x = layers.Dense(64, activation="relu")(x)
    out = layers.Dense(input_dim, activation="linear")(x)

    model = keras.Model(inp, out, name="sentinelx_autoencoder")
    model.compile(optimizer=keras.optimizers.Adam(1e-3), loss="mse")
    return model


def train_keras(x_train, x_val, input_dim, epochs=100, batch_size=256, verbose=True):
    model = build_keras_autoencoder(input_dim)
    callbacks = []
    if x_val is not None and len(x_val) > 0:
        from tensorflow.keras.callbacks import EarlyStopping
        callbacks.append(EarlyStopping(monitor="val_loss", patience=10, restore_best_weights=True))
    model.fit(
        x_train, x_train,
        validation_data=(x_val, x_val) if x_val is not None and len(x_val) > 0 else None,
        epochs=epochs,
        batch_size=batch_size,
        shuffle=True,
        verbose=1 if verbose else 0,
        callbacks=callbacks,
    )
    return model


def reconstruction_error_np(model, x):
    rec = model.predict(x, verbose=0)
    return np.mean((x - rec) ** 2, axis=1)
