import Icon from './Icon'

export default function Pagination({ page, totalPages, totalElements, onPageChange, disabled }) {
  const current = totalPages === 0 ? 1 : page + 1
  const pages = Math.max(totalPages, 1)

  return (
    <div className="pagination">
      <span className="pagination-info">
        Page {current} of {pages} · {totalElements} {totalElements === 1 ? 'customer' : 'customers'}
      </span>
      <div className="pagination-actions">
        <button
          type="button"
          className="btn btn-tonal btn-sm"
          onClick={() => onPageChange(page - 1)}
          disabled={disabled || page <= 0}
        >
          <Icon name="prev" size={18} />
          Previous
        </button>
        <button
          type="button"
          className="btn btn-tonal btn-sm"
          onClick={() => onPageChange(page + 1)}
          disabled={disabled || page + 1 >= totalPages}
        >
          Next
          <Icon name="next" size={18} />
        </button>
      </div>
    </div>
  )
}
