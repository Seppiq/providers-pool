package com.example.providers.service;

import com.example.providers.domain.Provider;
import com.example.providers.source.ProviderSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderRegistryTest {

    private static final Provider A = new Provider("a", "https://a.example.com", "key-a");

    @Mock
    private ProviderSource source;

    @Test
    void shouldLoadProvidersFromSourceAtStartup() {
        when(source.loadProviders()).thenReturn(List.of(
                A,
                new Provider("b", "https://b.example.com", "key-b")));

        ProviderRegistry registry = new ProviderRegistry(source);

        assertThat(registry.findById("a")).contains(A);
        assertThat(registry.findById("missing")).isEmpty();
        assertThat(registry.getAll()).hasSize(2);
    }

    @Test
    void shouldReplaceWholeSetAndSaveItToSource() {
        when(source.loadProviders()).thenReturn(List.of(A));
        ProviderRegistry registry = new ProviderRegistry(source);

        List<Provider> newSet = List.of(
                new Provider("x", "https://x.example.com", "key-x"),
                new Provider("y", "https://y.example.com", "key-y"));
        registry.replaceAll(newSet);

        verify(source).saveProviders(newSet);
        assertThat(registry.findById("a")).isEmpty();
        assertThat(registry.getAll())
                .extracting(Provider::id)
                .containsExactlyInAnyOrder("x", "y");
    }

    @Test
    void shouldKeepCurrentSetInMemoryWhenSavingFails() {
        when(source.loadProviders()).thenReturn(List.of(A));
        ProviderRegistry registry = new ProviderRegistry(source);
        doThrow(new UncheckedIOException(new IOException("disk full")))
                .when(source).saveProviders(any());

        assertThatThrownBy(() -> registry.replaceAll(List.of(
                new Provider("x", "https://x.example.com", "key-x"))))
                .isInstanceOf(UncheckedIOException.class);

        assertThat(registry.findById("a")).contains(A);
        assertThat(registry.findById("x")).isEmpty();
    }

    @Test
    void shouldRejectDuplicateIdsWithoutSavingToSource() {
        when(source.loadProviders()).thenReturn(List.of(A));
        ProviderRegistry registry = new ProviderRegistry(source);

        List<Provider> duplicates = List.of(
                new Provider("x", "https://x.example.com", "key-x"),
                new Provider("x", "https://same-id.example.com", "key-x-2"));

        assertThatThrownBy(() -> registry.replaceAll(duplicates))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("x");

        // Невалидный список не должен был дойти до источника и не подменяет набор в памяти.
        verify(source, never()).saveProviders(any());
        assertThat(registry.findById("a")).contains(A);
        assertThat(registry.findById("x")).isEmpty();
    }

    @Test
    void shouldFailFastOnDuplicateIdsAtStartup() {
        when(source.loadProviders()).thenReturn(List.of(
                A,
                new Provider("a", "https://other.example.com", "key-a-2")));

        assertThatThrownBy(() -> new ProviderRegistry(source))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("a");
    }

    @Test
    void concurrentReplacementsShouldKeepMemoryConsistentWithSource() throws Exception {
        List<Provider> initialSet = List.of(new Provider("init", "https://init.example.com", "k"));
        AtomicReference<List<Provider>> lastSaved = new AtomicReference<>(initialSet);
        ProviderSource countingSource = new ProviderSource() {
            @Override
            public List<Provider> loadProviders() {
                return initialSet;
            }

            @Override
            public void saveProviders(List<Provider> providers) {
                lastSaved.set(providers);
            }
        };
        ProviderRegistry registry = new ProviderRegistry(countingSource);

        // Без ExecutorService: он стал AutoCloseable только в JDK 19, на релизе 17
        // try-with-resources из подсказки IDEA просто не соберётся. Потоки проще.
        int threadsCount = 4;
        int replacementsPerThread = 50;
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(threadsCount);
        for (int t = 0; t < threadsCount; t++) {
            int threadNumber = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < replacementsPerThread; i++) {
                        registry.replaceAll(List.of(
                                new Provider("t" + threadNumber + "-" + i, "https://x.example.com", "k")));
                    }
                } catch (Throwable e) {
                    failure.compareAndSet(null, e);
                } finally {
                    done.countDown();
                }
            }).start();
        }

        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(failure.get()).isNull();
        // Память и источник содержат ровно один и тот же полный набор — без смешения замен.
        assertThat(registry.getAll()).containsExactlyElementsOf(lastSaved.get());
    }
}
