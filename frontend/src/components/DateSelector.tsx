import { useState } from 'react';

export type DateSelectorProps = {
  date: string;
  onChange: (date: string) => void;
};

const COMPLETE_DATE = /^\d{4}-\d{2}-\d{2}$/;

export default function DateSelector({ date, onChange }: DateSelectorProps) {
  const [draft, setDraft] = useState(date);
  const [lastDate, setLastDate] = useState(date);

  if (date !== lastDate) {
    setLastDate(date);
    setDraft(date);
  }

  return (
    <input
      aria-label="날짜 선택"
      value={draft}
      onChange={(e) => {
        const next = e.target.value;
        setDraft(next);
        if (COMPLETE_DATE.test(next)) {
          onChange(next);
        }
      }}
    />
  );
}
