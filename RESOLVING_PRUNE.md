# Resolving Large Files in Git History

This document outlines the steps taken to remove large files from the Git history, thereby reducing the repository size and resolving issues with pushing large files.

## Problem Identification

The primary issue was the presence of large files within the Git history, which prevented successful pushes to the remote repository. Although these files were added to `.gitignore` and were not present in the current working directory or Git index, they still existed in older commits.

The following command was used to identify the top 20 largest files in the Git history:

```bash
git rev-list --objects --all | git cat-file --batch-check='%(objecttype) %(objectname) %(objectsize) %(rest)' | sed -n 's/^blob //p' | sort --numeric-sort --key=2 | tail -n 20
```

This command helped pinpoint the specific large files and the directories they resided in, such as `whisper-models`, `sounds`, and `.quinoa`.

## Solution: Rewriting Git History

To permanently remove these large files from the Git history, the `git filter-branch` command was used. This command rewrites the repository's history, effectively creating a new history without the specified files.

**Warning**: `git filter-branch` is a destructive operation that rewrites your commit history. It should be used with caution, especially in shared repositories. After this operation, all collaborators will need to re-clone the repository or rebase their work.

The command executed was:

```bash
git filter-branch --force --index-filter 'git rm --cached --ignore-unmatch -r whisper-models sounds .quinoa' --prune-empty --tag-name-filter cat -- --all
```

- `--force`: Allows the operation to run even if the history is already rewritten.
- `--index-filter '<command>'`: This command is applied to each commit's index. Here, `git rm --cached --ignore-unmatch -r <directories>` removes the specified directories from the index of every commit.
- `--prune-empty`: Removes commits that become empty after the filter operation.
- `--tag-name-filter cat`: Rewrites tags to point to the new, filtered commits.
- `-- --all`: Applies the filter to all branches and tags.

## Cleaning Up the Repository

After rewriting the history, Git retains references to the old, unreferenced commits. To truly reduce the repository size, these old objects need to be purged. This was achieved by:

1.  **Expiring the reflog:** This removes pointers to the old commits.
    ```bash
    git reflog expire --expire=now --all
    ```

2.  **Deleting original refs:** `git filter-branch` creates backups of the original refs under `refs/original/`. These must be removed to allow garbage collection to prune the old objects.
    ```bash
    git for-each-ref --format="delete %(refname)" refs/original | git update-ref --stdin
    ```

3.  **Running aggressive garbage collection:** This command removes all unreachable objects from the repository.
    ```bash
    git gc --prune=now --aggressive
    ```

## Verification

After the cleanup, the repository size was verified by checking the size of the `.git` directory:

```bash
du -sh .git
```

This showed a significant reduction in size (from 2.0GB to 460KB).

## Pushing Changes

Since the Git history was rewritten, a regular `git push` would be rejected. Therefore, a force push was necessary to update the remote repository:

```bash
git push --force
```

**Note**: Force pushing overwrites the remote history. All collaborators must re-clone the repository after this operation.
