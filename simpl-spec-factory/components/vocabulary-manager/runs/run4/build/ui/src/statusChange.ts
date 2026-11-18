import { updateVocabularyStatus } from './api';
import { notifyError, notifySuccess } from './notifications';
import { otherStatus, statusLabel } from './labels';
import { describeError, statusOf } from './errors';
import type { Vocabulary } from './types';

/**
 * The status-change action (SIMPL-18174): offers only the other status (D4). Returns the updated vocabulary, or
 * undefined when nothing changed.
 */
export async function changeStatus(vocabulary: Vocabulary): Promise<Vocabulary | undefined> {
  const target = otherStatus(vocabulary.status);
  try {
    const updated = await updateVocabularyStatus(vocabulary.name, target);
    notifySuccess(`The status of ${vocabulary.name} was updated to ${statusLabel(updated.status)}.`);
    return updated;
  } catch (error) {
    if (statusOf(error) === 404) {
      notifyError('Vocabulary not found. The vocabulary could not be found.');
    } else {
      const shown = describeError(error, 'The vocabulary status could not be updated.');
      notifyError(
        shown.message === 'The vocabulary status could not be updated.'
          ? shown.message
          : `The vocabulary status could not be updated. ${shown.message}`,
        shown.detail,
      );
    }
    return undefined;
  }
}

export function statusActionLabel(vocabulary: Vocabulary): string {
  return `Change status to ${statusLabel(otherStatus(vocabulary.status))}`;
}
