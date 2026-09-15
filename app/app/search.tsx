import { useEffect, useState, useCallback } from 'react';
import { View, Text, Pressable, FlatList, ActivityIndicator, StyleSheet, TextInput } from 'react-native';
import { useRouter, Stack } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import Svg, { Path, Circle } from 'react-native-svg';
import ResourceCard from '../src/components/ResourceCard';
import type { ResourceCardItem } from '../src/components/ResourceCard';
import { resourceApi, ApiError } from '../src/api/client';
import { toResourceCardItem } from '../src/utils/mapResource';
import { useBookmarks } from '../src/contexts/BookmarkContext';
import { categories } from '../src/constants/categories';
import { colors, radius, fonts } from '../src/theme/tokens';

const PAGE_SIZE = 10;

type SortOption = 'latest' | 'freeOnly';

export default function SearchScreen() {
  const router = useRouter();
  const { bookmarked, toggle } = useBookmarks();

  const [keyword, setKeyword] = useState('');
  const [activeCategory, setActiveCategory] = useState('ALL');
  const [freeOnly, setFreeOnly] = useState(false);

  const [items, setItems] = useState<ResourceCardItem[]>([]);
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadPage = useCallback((pageToLoad: number, replace: boolean) => {
    const setLoadingFlag = replace ? setLoading : setLoadingMore;
    setLoadingFlag(true);
    setError(null);

    resourceApi
      .search({
        category: activeCategory === 'ALL' ? undefined : activeCategory,
        keyword: keyword.trim() || undefined,
        page: pageToLoad,
        size: PAGE_SIZE,
      })
      .then((result) => {
        const mapped = result.content.map((r) => toResourceCardItem(r));
        setItems((prev) => (replace ? mapped : [...prev, ...mapped]));
        setTotalElements(result.totalElements);
        setPage(pageToLoad);
      })
      .catch((e) => setError(e instanceof ApiError ? e.message : '물품을 불러오지 못했어요.'))
      .finally(() => setLoadingFlag(false));
  }, [activeCategory, keyword]);

  useEffect(() => {
    loadPage(0, true);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeCategory]);

  const handleSearchSubmit = () => loadPage(0, true);
  const handleLoadMore = () => loadPage(page + 1, false);

  const visibleItems = freeOnly ? items.filter((it) => it.fee === '무료') : items;
  const hasMore = (page + 1) * PAGE_SIZE < totalElements;

  return (
    <View style={{ flex: 1, backgroundColor: colors.surface }}>
      <Stack.Screen options={{ headerShown: true, title: '검색', headerBackTitle: '홈' }} />

      <View style={styles.searchBarWrap}>
        <View style={styles.searchBar}>
          <Svg width={16} height={16} viewBox="0 0 24 24" fill="none" stroke={colors.ink3} strokeWidth={2.2}>
            <Circle cx="11" cy="11" r="7" /><Path d="M21 21l-4.3-4.3" />
          </Svg>
          <TextInput
            style={styles.searchInput}
            placeholder="무엇이 필요하세요?"
            placeholderTextColor={colors.ink3}
            value={keyword}
            onChangeText={setKeyword}
            onSubmitEditing={handleSearchSubmit}
            returnKeyType="search"
          />
          {keyword.length > 0 && (
            <Pressable onPress={() => { setKeyword(''); loadPage(0, true); }}>
              <Ionicons name="close-circle" size={16} color={colors.ink3} />
            </Pressable>
          )}
        </View>
      </View>

      <View style={styles.filterRow}>
        <FlatList
          horizontal
          showsHorizontalScrollIndicator={false}
          data={categories}
          keyExtractor={(c) => c.key}
          contentContainerStyle={{ paddingHorizontal: 16, gap: 8 }}
          renderItem={({ item: c }) => {
            const active = c.key === activeCategory;
            return (
              <Pressable
                style={[styles.chip, active && styles.chipActive]}
                onPress={() => setActiveCategory(c.key)}
              >
                <Text style={[styles.chipText, active && styles.chipTextActive]}>{c.label}</Text>
              </Pressable>
            );
          }}
        />
      </View>

      <View style={styles.filterRow2}>
        <Pressable
          style={[styles.toggleChip, freeOnly && styles.toggleChipActive]}
          onPress={() => setFreeOnly((v) => !v)}
        >
          <Text style={[styles.toggleChipText, freeOnly && styles.toggleChipTextActive]}>무료만</Text>
        </Pressable>
        <Text style={styles.resultCount}>
          검색 결과 <Text style={{ color: colors.brand, fontFamily: fonts.bold }}>{totalElements}건</Text>
        </Text>
      </View>

      {loading && (
        <View style={styles.center}><ActivityIndicator color={colors.brand} /></View>
      )}

      {!loading && error && (
        <StateView title="연결이 원활하지 않아요" desc="네트워크 상태를 확인하고 다시 시도해주세요." actionLabel="다시 시도" onAction={() => loadPage(0, true)} />
      )}

      {!loading && !error && visibleItems.length === 0 && (
        <StateView title="검색 결과가 없어요" desc="다른 검색어나 카테고리로 시도해보세요." actionLabel="필터 초기화" onAction={() => { setKeyword(''); setActiveCategory('ALL'); setFreeOnly(false); }} />
      )}

      {!loading && !error && visibleItems.length > 0 && (
        <FlatList
          data={visibleItems}
          keyExtractor={(it) => it.id}
          contentContainerStyle={{ padding: 20, paddingTop: 4 }}
          renderItem={({ item }) => (
            <ResourceCard
              item={{ ...item, bookmarked: bookmarked.has(item.id) }}
              onPress={() => router.push(`/resources/${item.id}`)}
              onToggleBookmark={() => toggle(item.id)}
            />
          )}
          ListFooterComponent={
            hasMore ? (
              <Pressable style={styles.loadMoreBtn} onPress={handleLoadMore} disabled={loadingMore}>
                {loadingMore ? (
                  <ActivityIndicator color={colors.brand} size="small" />
                ) : (
                  <Text style={styles.loadMoreText}>물품 더 보기</Text>
                )}
              </Pressable>
            ) : null
          }
        />
      )}
    </View>
  );
}

function StateView({ title, desc, actionLabel, onAction }: {
  title: string; desc: string; actionLabel: string; onAction: () => void;
}) {
  return (
    <View style={styles.stateView}>
      <View style={styles.stateIcon}>
        <Svg width={28} height={28} viewBox="0 0 24 24" fill="none" stroke={colors.ink3} strokeWidth={2}>
          <Circle cx="11" cy="11" r="7" /><Path d="M21 21l-4.3-4.3" />
        </Svg>
      </View>
      <Text style={styles.stateTitle}>{title}</Text>
      <Text style={styles.stateDesc}>{desc}</Text>
      <Pressable onPress={onAction} style={styles.stateBtn}>
        <Text style={styles.stateBtnText}>{actionLabel}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  searchBarWrap: { paddingHorizontal: 16, paddingTop: 12, paddingBottom: 10 },
  searchBar: {
    height: 46, borderRadius: radius.lg, borderWidth: 1.5, borderColor: colors.brand,
    backgroundColor: colors.surface, flexDirection: 'row', alignItems: 'center', gap: 8, paddingHorizontal: 14,
  },
  searchInput: { flex: 1, fontSize: 13, fontFamily: fonts.regular, color: colors.ink, padding: 0 },
  filterRow: { paddingBottom: 10 },
  chip: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: radius.full, borderWidth: 1, borderColor: colors.line },
  chipActive: { backgroundColor: colors.brand, borderWidth: 0 },
  chipText: { fontSize: 12.5, fontFamily: fonts.regular, color: colors.ink2 },
  chipTextActive: { color: '#fff', fontFamily: fonts.semibold },
  filterRow2: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingBottom: 10 },
  toggleChip: { paddingHorizontal: 12, paddingVertical: 6, borderRadius: radius.full, borderWidth: 1, borderColor: colors.line },
  toggleChipActive: { backgroundColor: colors.brand, borderWidth: 0 },
  toggleChipText: { fontSize: 12, fontFamily: fonts.regular, color: colors.ink2 },
  toggleChipTextActive: { color: '#fff', fontFamily: fonts.semibold },
  resultCount: { fontSize: 12, fontFamily: fonts.regular, color: colors.ink3 },
  loadMoreBtn: { height: 48, borderRadius: radius.md, borderWidth: 1, borderColor: colors.line, alignItems: 'center', justifyContent: 'center', marginTop: 8 },
  loadMoreText: { fontSize: 13, fontFamily: fonts.semibold, color: colors.ink2 },
  stateView: { flex: 1, alignItems: 'center', paddingTop: 90, paddingHorizontal: 40 },
  stateIcon: { width: 64, height: 64, borderRadius: 32, backgroundColor: colors.grayFill, alignItems: 'center', justifyContent: 'center', marginBottom: 18 },
  stateTitle: { fontSize: 15, fontFamily: fonts.bold, marginBottom: 8, color: colors.ink },
  stateDesc: { fontSize: 12.5, fontFamily: fonts.regular, color: colors.ink3, textAlign: 'center', lineHeight: 18, marginBottom: 20 },
  stateBtn: { paddingHorizontal: 18, paddingVertical: 10, borderRadius: 12, borderWidth: 1.5, borderColor: colors.line },
  stateBtnText: { fontSize: 13, fontFamily: fonts.bold, color: colors.ink },
});