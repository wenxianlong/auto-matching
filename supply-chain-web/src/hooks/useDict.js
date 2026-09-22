import { useState, useEffect } from 'react';
import { getDictDataApi } from '../api/dict';

// 全局内存缓存，页面刷新前一直有效
const dictCache = new Map();

export const useDict = (...dictTypes) => {
    const [dicts, setDicts] = useState({});
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const fetchDicts = async () => {
            setLoading(true);
            const promises = dictTypes.map(async (type) => {
                if (dictCache.has(type)) {
                    return { type, data: dictCache.get(type) };
                }
                const res = await getDictDataApi({dictType:type});
                const data = res || [];
                dictCache.set(type, data);
                return { type, data };
            });

            const results = await Promise.all(promises);
            const newDicts = {};
            results.forEach(r => { newDicts[r.type] = r.data; });
            setDicts(newDicts);
            setLoading(false);
        };
        fetchDicts();
    }, [dictTypes.join(',')]);

    return [dicts, loading];
};
